import json
from pathlib import Path

import httpx

from lyftix_workers.github.client import GitHubClient
from lyftix_workers.github.ingestion import GitHubIngestionJob
from lyftix_workers.lyftix_client import LyftixClient
from lyftix_workers.state import CheckpointStore


def test_github_response_maps_to_canonical_lyftix_request() -> None:
    posted: list[dict] = []

    def github_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            request=request,
            json=[
                {
                    "id": "github-42",
                    "type": "PullRequestEvent",
                    "repo": {"name": "octocat/lyftix"},
                    "created_at": "2026-09-07T12:00:00Z",
                    "payload": {
                        "action": "closed",
                        "pull_request": {"title": "Ship ingestion", "merged": True},
                    },
                }
            ],
        )

    def lyftix_handler(request: httpx.Request) -> httpx.Response:
        posted.append(json.loads(request.content))
        return httpx.Response(201, request=request)

    with (
        httpx.Client(transport=httpx.MockTransport(github_handler)) as github_http,
        httpx.Client(transport=httpx.MockTransport(lyftix_handler)) as lyftix_http,
    ):
        summary = GitHubIngestionJob(
            GitHubClient(github_http, "https://api.github.test", "secret", "octocat", 30),
            LyftixClient(lyftix_http, "http://lyftix.test"),
        ).run()

    assert summary.created == 1
    assert posted == [
        {
            "activityType": "PULL_REQUEST_MERGED",
            "repositoryName": "lyftix",
            "repositoryOwner": "octocat",
            "occurredAt": "2026-09-07T12:00:00Z",
            "externalId": "github-42",
            "title": "Ship ingestion",
        }
    ]


def test_paginated_checkpointed_flow_does_not_repost_on_second_run(tmp_path: Path) -> None:
    posted: list[str] = []

    def github_handler(request: httpx.Request) -> httpx.Response:
        if request.url.params.get("page") == "2":
            return httpx.Response(200, request=request, json=[push_event("oldest")])
        return httpx.Response(
            200,
            request=request,
            json=[push_event("newest"), push_event("middle")],
            headers={"link": '<https://api.github.test/events?page=2>; rel="next"'},
        )

    def lyftix_handler(request: httpx.Request) -> httpx.Response:
        posted.append(json.loads(request.content)["externalId"])
        return httpx.Response(201, request=request)

    store = CheckpointStore(tmp_path / "github.json")
    with (
        httpx.Client(transport=httpx.MockTransport(github_handler)) as github_http,
        httpx.Client(transport=httpx.MockTransport(lyftix_handler)) as lyftix_http,
    ):
        job = GitHubIngestionJob(
            GitHubClient(github_http, "https://api.github.test", "secret", "octocat", 2, 3),
            LyftixClient(lyftix_http, "http://lyftix.test"),
            store,
        )
        first = job.run()
        second = job.run()

    assert first.created == 3
    assert second.fetched == 0
    assert posted == ["newest", "middle", "oldest"]
    assert store.load() is not None
    assert store.load().event_id == "newest"


def push_event(event_id: str) -> dict:
    return {
        "id": event_id,
        "type": "PushEvent",
        "repo": {"name": "octocat/lyftix"},
        "created_at": "2026-09-07T12:00:00Z",
        "payload": {"ref": "refs/heads/main", "commits": [{}]},
    }
