from __future__ import annotations

from typing import Any

import httpx


class GitHubApiError(RuntimeError):
    """Raised when GitHub cannot provide an events response."""


class GitHubClient:
    def __init__(
        self,
        http_client: httpx.Client,
        base_url: str,
        token: str,
        username: str,
        events_per_page: int,
    ) -> None:
        self._http_client = http_client
        self._base_url = base_url.rstrip("/")
        self._username = username
        self._events_per_page = events_per_page
        self._headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "lyftix-worker/0.1.0",
        }

    def fetch_recent_events(self) -> list[dict[str, Any]]:
        url = f"{self._base_url}/users/{self._username}/events/public"
        try:
            response = self._http_client.get(
                url,
                headers=self._headers,
                params={"per_page": self._events_per_page},
            )
        except httpx.HTTPError as exc:
            raise GitHubApiError(f"GitHub request failed: {exc}") from exc

        if response.status_code >= 400:
            remaining = response.headers.get("x-ratelimit-remaining")
            reset = response.headers.get("x-ratelimit-reset")
            rate_detail = ""
            if response.status_code in {403, 429} and remaining == "0":
                rate_detail = f"; rate limit exhausted, reset={reset or 'unknown'}"
            raise GitHubApiError(
                f"GitHub returned HTTP {response.status_code}{rate_detail}"
            )

        try:
            payload = response.json()
        except ValueError as exc:
            raise GitHubApiError("GitHub returned invalid JSON") from exc
        if not isinstance(payload, list):
            raise GitHubApiError("GitHub events response was not a list")
        return payload
