import json

import httpx

from lyftix_workers.github.client import GitHubClient
from lyftix_workers.github.ingestion import GitHubIngestionJob
from lyftix_workers.lyftix_client import LyftixClient


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
