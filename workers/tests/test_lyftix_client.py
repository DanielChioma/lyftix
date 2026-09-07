import httpx
import pytest

from lyftix_workers.github.mapper import CanonicalGitHubActivity
from lyftix_workers.lyftix_client import IngestionOutcome, LyftixApiError, LyftixClient


def activity() -> CanonicalGitHubActivity:
    return CanonicalGitHubActivity(
        activity_type="PUSH",
        repository_name="lyftix",
        repository_owner="octocat",
        occurred_at="2026-09-07T12:00:00Z",
        external_id="event-123",
        title="Pushed 1 commit(s) to main",
    )


@pytest.mark.parametrize(
    ("status_code", "expected"),
    [(201, IngestionOutcome.CREATED), (409, IngestionOutcome.DUPLICATE)],
)
def test_classifies_success_and_duplicate(status_code: int, expected: IngestionOutcome) -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(status_code, request=request))
    with httpx.Client(transport=transport) as http_client:
        result = LyftixClient(http_client, "http://lyftix.test").create_github_activity(activity())

    assert result is expected


@pytest.mark.parametrize("status_code", [400, 422, 500, 503])
def test_other_client_and_server_responses_are_failures(status_code: int) -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(status_code, request=request))
    with (
        httpx.Client(transport=transport) as http_client,
        pytest.raises(LyftixApiError, match=f"Lyftix returned HTTP {status_code}"),
    ):
            LyftixClient(http_client, "http://lyftix.test").create_github_activity(activity())
