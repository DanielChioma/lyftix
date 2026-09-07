from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

import httpx


class GitHubApiError(RuntimeError):
    """Raised when GitHub cannot provide an events response."""


LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class GitHubFetchResult:
    events: list[dict[str, Any]]
    pages_fetched: int
    checkpoint_reached: bool
    history_exhausted: bool


class GitHubClient:
    def __init__(
        self,
        http_client: httpx.Client,
        base_url: str,
        token: str,
        username: str,
        events_per_page: int,
        max_pages_per_run: int = 1,
    ) -> None:
        self._http_client = http_client
        self._base_url = base_url.rstrip("/")
        self._username = username
        self._events_per_page = events_per_page
        self._max_pages_per_run = max_pages_per_run
        self._headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "lyftix-worker/0.1.0",
        }

    def fetch_recent_events(self) -> list[dict[str, Any]]:
        return self.fetch_since(None).events

    def fetch_since(self, checkpoint_event_id: str | None) -> GitHubFetchResult:
        url: str | None = f"{self._base_url}/users/{self._username}/events/public"
        params: dict[str, int] | None = {"per_page": self._events_per_page}
        events: list[dict[str, Any]] = []
        checkpoint_reached = False
        history_exhausted = False

        for page_number in range(1, self._max_pages_per_run + 1):
            LOGGER.info("Fetching GitHub events page=%d", page_number)
            response = self._request_page(url, params)
            params = None
            try:
                payload = response.json()
            except ValueError as exc:
                raise GitHubApiError("GitHub returned invalid JSON") from exc
            if not isinstance(payload, list):
                raise GitHubApiError("GitHub events response was not a list")

            for event in payload:
                if not isinstance(event, dict):
                    raise GitHubApiError("GitHub events response contained a non-object event")
                if checkpoint_event_id is not None and event.get("id") == checkpoint_event_id:
                    checkpoint_reached = True
                    LOGGER.info("Reached GitHub checkpoint eventId=%s", checkpoint_event_id)
                    break
                events.append(event)
            if checkpoint_reached:
                break

            next_link = response.links.get("next", {}).get("url")
            if not payload or len(payload) < self._events_per_page or not next_link:
                history_exhausted = True
                break
            url = next_link
        else:
            LOGGER.info("Stopped GitHub pagination at maxPages=%d", self._max_pages_per_run)

        return GitHubFetchResult(
            events=events,
            pages_fetched=page_number,
            checkpoint_reached=checkpoint_reached,
            history_exhausted=history_exhausted,
        )

    def _request_page(
        self,
        url: str,
        params: dict[str, int] | None,
    ) -> httpx.Response:
        try:
            response = self._http_client.get(
                url,
                headers=self._headers,
                params=params,
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

        return response
