from __future__ import annotations

from enum import Enum

import httpx

from lyftix_workers.github.mapper import CanonicalGitHubActivity


class IngestionOutcome(Enum):
    CREATED = "created"
    DUPLICATE = "duplicate"


class LyftixApiError(RuntimeError):
    """Raised when Lyftix rejects or cannot persist an activity."""


class LyftixClient:
    def __init__(self, http_client: httpx.Client, base_url: str) -> None:
        self._http_client = http_client
        self._url = f"{base_url.rstrip('/')}/api/github-activities"

    def create_github_activity(self, activity: CanonicalGitHubActivity) -> IngestionOutcome:
        try:
            response = self._http_client.post(self._url, json=activity.to_backend_payload())
        except httpx.HTTPError as exc:
            raise LyftixApiError(f"Lyftix request failed: {exc}") from exc

        if response.status_code == 201:
            return IngestionOutcome.CREATED
        if response.status_code == 409:
            return IngestionOutcome.DUPLICATE
        raise LyftixApiError(f"Lyftix returned HTTP {response.status_code}")
