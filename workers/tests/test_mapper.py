import pytest

from lyftix_workers.github.mapper import map_github_event


def event(event_type: str, payload: dict) -> dict:
    return {
        "id": "event-123",
        "type": event_type,
        "repo": {"name": "octocat/lyftix"},
        "created_at": "2026-09-07T12:00:00Z",
        "payload": payload,
    }


def test_maps_push_event() -> None:
    result = map_github_event(event("PushEvent", {"ref": "refs/heads/main", "commits": [{}, {}]}))

    assert result is not None
    assert result.activity_type == "PUSH"
    assert result.title == "Pushed 2 commit(s) to main"
    assert result.external_id == "event-123"


@pytest.mark.parametrize(
    ("action", "merged", "expected"),
    [
        ("opened", False, "PULL_REQUEST_OPENED"),
        ("closed", False, "PULL_REQUEST_CLOSED"),
        ("closed", True, "PULL_REQUEST_MERGED"),
    ],
)
def test_maps_pull_request_events(action: str, merged: bool, expected: str) -> None:
    result = map_github_event(
        event(
            "PullRequestEvent",
            {"action": action, "pull_request": {"title": "PR title", "merged": merged}},
        )
    )

    assert result is not None
    assert result.activity_type == expected
    assert result.title == "PR title"


@pytest.mark.parametrize(
    ("action", "expected"),
    [("opened", "ISSUE_OPENED"), ("closed", "ISSUE_CLOSED")],
)
def test_maps_issue_events(action: str, expected: str) -> None:
    result = map_github_event(
        event("IssuesEvent", {"action": action, "issue": {"title": "Issue title"}})
    )

    assert result is not None
    assert result.activity_type == expected


@pytest.mark.parametrize(
    ("ref_type", "ref", "expected"),
    [
        ("repository", None, "REPOSITORY_CREATED"),
        ("branch", "feature", "BRANCH_CREATED"),
        ("tag", "v1.0.0", "TAG_CREATED"),
    ],
)
def test_maps_create_events(ref_type: str, ref: str | None, expected: str) -> None:
    result = map_github_event(event("CreateEvent", {"ref_type": ref_type, "ref": ref}))

    assert result is not None
    assert result.activity_type == expected


def test_maps_published_release() -> None:
    result = map_github_event(
        event(
            "ReleaseEvent",
            {
                "action": "published",
                "release": {"name": "Lyftix 1.0", "tag_name": "v1.0"},
            },
        )
    )

    assert result is not None
    assert result.activity_type == "RELEASE_PUBLISHED"
    assert result.title == "Lyftix 1.0"


def test_skips_unsupported_event() -> None:
    assert map_github_event(event("WatchEvent", {})) is None


@pytest.mark.parametrize(
    "malformed",
    [
        event("PushEvent", {"ref": "refs/heads/main"}),
        event("PullRequestEvent", {"action": "opened", "pull_request": {}}),
        event("CreateEvent", {"ref_type": "branch", "ref": None}),
        {"id": "1", "type": "PushEvent", "payload": {}},
    ],
)
def test_skips_insufficient_supported_payload(malformed: dict) -> None:
    assert map_github_event(malformed) is None
