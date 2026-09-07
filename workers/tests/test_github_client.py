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
