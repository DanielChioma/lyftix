from unittest.mock import Mock

import pytest

from lyftix_workers.github.client import GitHubClient
from lyftix_workers.github.ingestion import GitHubIngestionJob
from lyftix_workers.lyftix_client import IngestionOutcome, LyftixApiError, LyftixClient


def github_event(event_id: str, event_type: str = "PushEvent") -> dict:
    return {
        "id": event_id,
        "type": event_type,
        "repo": {"name": "octocat/lyftix"},
        "created_at": "2026-09-07T12:00:00Z",
        "payload": {"ref": "refs/heads/main", "commits": [{}]},
    }


@pytest.mark.parametrize(
    ("outcome", "created", "duplicates"),
    [(IngestionOutcome.CREATED, 1, 0), (IngestionOutcome.DUPLICATE, 0, 1)],
)
def test_counts_success_and_duplicate(
    outcome: IngestionOutcome,
    created: int,
    duplicates: int,
) -> None:
    github_client = Mock(spec=GitHubClient)
    lyftix_client = Mock(spec=LyftixClient)
    github_client.fetch_recent_events.return_value = [github_event("1")]
    lyftix_client.create_github_activity.return_value = outcome

    summary = GitHubIngestionJob(github_client, lyftix_client).run()

    assert summary.fetched == 1
    assert summary.mapped == 1
    assert summary.created == created
    assert summary.duplicates == duplicates
    assert summary.skipped == 0
    assert summary.failed == 0
    assert summary.successful


def test_skips_unsupported_event_without_posting() -> None:
    github_client = Mock(spec=GitHubClient)
    lyftix_client = Mock(spec=LyftixClient)
    github_client.fetch_recent_events.return_value = [github_event("1", "WatchEvent")]

    summary = GitHubIngestionJob(github_client, lyftix_client).run()

    assert summary.fetched == 1
    assert summary.mapped == 0
    assert summary.skipped == 1
    assert summary.successful
    lyftix_client.create_github_activity.assert_not_called()


def test_mixed_batch_continues_after_duplicate_and_failure() -> None:
    github_client = Mock(spec=GitHubClient)
    lyftix_client = Mock(spec=LyftixClient)
    github_client.fetch_recent_events.return_value = [
        github_event("created"),
        github_event("duplicate"),
        github_event("unsupported", "WatchEvent"),
        github_event("failed"),
    ]
    lyftix_client.create_github_activity.side_effect = [
        IngestionOutcome.CREATED,
        IngestionOutcome.DUPLICATE,
        LyftixApiError("backend unavailable"),
    ]

    summary = GitHubIngestionJob(github_client, lyftix_client).run()

    assert summary.fetched == 4
    assert summary.mapped == 3
    assert summary.created == 1
    assert summary.duplicates == 1
    assert summary.skipped == 1
    assert summary.failed == 1
    assert not summary.successful
    assert lyftix_client.create_github_activity.call_count == 3
