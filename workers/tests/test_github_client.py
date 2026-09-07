import httpx
import pytest

from lyftix_workers.github.client import GitHubApiError, GitHubClient


def test_fetches_events_with_authentication_and_github_headers() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["request"] = request
        return httpx.Response(200, json=[{"id": "1"}])

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        events = GitHubClient(http_client, "https://api.github.test", "secret", "octocat", 25)
        result = events.fetch_recent_events()

    request = captured["request"]
    assert isinstance(request, httpx.Request)
    assert result == [{"id": "1"}]
    assert request.url.path == "/users/octocat/events/public"
    assert request.url.params["per_page"] == "25"
    assert request.headers["authorization"] == "Bearer secret"
    assert request.headers["accept"] == "application/vnd.github+json"
    assert request.headers["x-github-api-version"] == "2022-11-28"
    assert request.headers["user-agent"] == "lyftix-worker/0.1.0"


def test_raises_useful_error_for_api_failure() -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(500, request=request))
    with httpx.Client(transport=transport) as http_client:
        client = GitHubClient(http_client, "https://api.github.test", "secret", "octocat", 30)
        with pytest.raises(GitHubApiError, match="GitHub returned HTTP 500"):
            client.fetch_recent_events()


def test_reports_rate_limit_exhaustion_without_exposing_token() -> None:
    transport = httpx.MockTransport(
        lambda request: httpx.Response(
            403,
            headers={"x-ratelimit-remaining": "0", "x-ratelimit-reset": "12345"},
            request=request,
        )
    )
    with httpx.Client(transport=transport) as http_client:
        client = GitHubClient(http_client, "https://api.github.test", "top-secret", "octocat", 30)
        with pytest.raises(GitHubApiError) as error:
            client.fetch_recent_events()

    assert "rate limit exhausted" in str(error.value)
    assert "12345" in str(error.value)
    assert "top-secret" not in str(error.value)


def test_fetches_multiple_pages_in_order_until_final_short_page() -> None:
    requested_pages: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        page = request.url.params.get("page", "1")
        requested_pages.append(page)
        if page == "1":
            return httpx.Response(
                200,
                request=request,
                json=[{"id": "3"}, {"id": "2"}],
                headers={"link": '<https://api.github.test/events?page=2>; rel="next"'},
            )
        return httpx.Response(200, request=request, json=[{"id": "1"}])

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        result = GitHubClient(
            http_client, "https://api.github.test", "secret", "octocat", 2, 3
        ).fetch_since(None)

    assert [item["id"] for item in result.events] == ["3", "2", "1"]
    assert requested_pages == ["1", "2"]
    assert result.pages_fetched == 2
    assert result.history_exhausted


def test_stops_on_empty_page() -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(200, request=request, json=[]))
    with httpx.Client(transport=transport) as http_client:
        result = GitHubClient(
            http_client, "https://api.github.test", "secret", "octocat", 30, 3
        ).fetch_since(None)

    assert result.events == []
    assert result.pages_fetched == 1
    assert result.history_exhausted


def test_stops_at_max_page_limit() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        page = request.url.params.get("page", "1")
        next_page = int(page) + 1
        return httpx.Response(
            200,
            request=request,
            json=[{"id": f"{page}-a"}, {"id": f"{page}-b"}],
            headers={"link": f'<https://api.github.test/events?page={next_page}>; rel="next"'},
        )

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        result = GitHubClient(
            http_client, "https://api.github.test", "secret", "octocat", 2, 2
        ).fetch_since(None)

    assert len(result.events) == 4
    assert result.pages_fetched == 2
    assert not result.history_exhausted


def test_stops_when_checkpoint_is_encountered() -> None:
    transport = httpx.MockTransport(
        lambda request: httpx.Response(
            200,
            request=request,
            json=[{"id": "new"}, {"id": "checkpoint"}, {"id": "old"}],
        )
    )
    with httpx.Client(transport=transport) as http_client:
        result = GitHubClient(
            http_client, "https://api.github.test", "secret", "octocat", 30, 3
        ).fetch_since("checkpoint")

    assert result.events == [{"id": "new"}]
    assert result.checkpoint_reached


def test_fails_if_later_page_returns_an_error() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.params.get("page") == "2":
            return httpx.Response(502, request=request)
        return httpx.Response(
            200,
            request=request,
            json=[{"id": "2"}, {"id": "1"}],
            headers={"link": '<https://api.github.test/events?page=2>; rel="next"'},
        )

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        client = GitHubClient(http_client, "https://api.github.test", "secret", "octocat", 2, 3)
        with pytest.raises(GitHubApiError, match="GitHub returned HTTP 502"):
            client.fetch_since(None)
