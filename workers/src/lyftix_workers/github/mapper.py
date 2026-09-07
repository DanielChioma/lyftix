from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any


@dataclass(frozen=True)
class CanonicalGitHubActivity:
    activity_type: str
    repository_name: str
    repository_owner: str
    occurred_at: str
    external_id: str
    title: str

    def to_backend_payload(self) -> dict[str, str]:
        values = asdict(self)
        return {
            "activityType": values["activity_type"],
            "repositoryName": values["repository_name"],
            "repositoryOwner": values["repository_owner"],
            "occurredAt": values["occurred_at"],
            "externalId": values["external_id"],
            "title": values["title"],
        }


def map_github_event(event: dict[str, Any]) -> CanonicalGitHubActivity | None:
    event_type = event.get("type")
    event_id = _text(event.get("id"))
    occurred_at = _text(event.get("created_at"))
    repository = event.get("repo")
    full_name = repository.get("name") if isinstance(repository, dict) else None
    if not event_id or not occurred_at or not isinstance(full_name, str) or "/" not in full_name:
        return None
    owner, repository_name = full_name.split("/", 1)
    if not owner or not repository_name:
        return None

    payload = event.get("payload")
    if not isinstance(payload, dict):
        return None

    classification = _classify(event_type, payload, full_name)
    if classification is None:
        return None
    activity_type, title = classification
    return CanonicalGitHubActivity(
        activity_type=activity_type,
        repository_name=repository_name,
        repository_owner=owner,
        occurred_at=occurred_at,
        external_id=event_id,
        title=title,
    )


def _classify(
    event_type: Any,
    payload: dict[str, Any],
    repository_full_name: str,
) -> tuple[str, str] | None:
    if event_type == "PushEvent":
        ref = _text(payload.get("ref"))
        commits = payload.get("commits")
        if not ref or not isinstance(commits, list):
            return None
        branch = ref.removeprefix("refs/heads/")
        return "PUSH", f"Pushed {len(commits)} commit(s) to {branch}"

    if event_type == "PullRequestEvent":
        action = payload.get("action")
        pull_request = payload.get("pull_request")
        if action not in {"opened", "closed"} or not isinstance(pull_request, dict):
            return None
        title = _text(pull_request.get("title"))
        if not title:
            return None
        if action == "opened":
            return "PULL_REQUEST_OPENED", title
        if pull_request.get("merged") is True:
            return "PULL_REQUEST_MERGED", title
        return "PULL_REQUEST_CLOSED", title

    if event_type == "IssuesEvent":
        action = payload.get("action")
        issue = payload.get("issue")
        if action not in {"opened", "closed"} or not isinstance(issue, dict):
            return None
        title = _text(issue.get("title"))
        if not title:
            return None
        return ("ISSUE_OPENED" if action == "opened" else "ISSUE_CLOSED"), title

    if event_type == "CreateEvent":
        ref_type = payload.get("ref_type")
        ref = _text(payload.get("ref"))
        if ref_type == "repository":
            return "REPOSITORY_CREATED", f"Created repository {repository_full_name}"
        if ref_type == "branch" and ref:
            return "BRANCH_CREATED", f"Created branch {ref}"
        if ref_type == "tag" and ref:
            return "TAG_CREATED", f"Created tag {ref}"
        return None

    if event_type == "ReleaseEvent":
        release = payload.get("release")
        if payload.get("action") != "published" or not isinstance(release, dict):
            return None
        name = _text(release.get("name")) or _text(release.get("tag_name"))
        if not name:
            return None
        return "RELEASE_PUBLISHED", name

    return None


def _text(value: Any) -> str | None:
    return value if isinstance(value, str) and value else None
