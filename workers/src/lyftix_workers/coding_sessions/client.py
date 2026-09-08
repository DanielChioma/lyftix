from __future__ import annotations

import httpx

from lyftix_workers.coding_sessions.parser import CodingSessionRecord


class CodingSessionApiError(RuntimeError):
    """Raised when Lyftix rejects or cannot persist a coding session."""


class CodingSessionClient:
    def __init__(self, http_client: httpx.Client, base_url: str) -> None:
        self._http_client = http_client
        self._url = f"{base_url.rstrip('/')}/api/coding-sessions"

    def create(self, session: CodingSessionRecord) -> None:
        try:
            response = self._http_client.post(self._url, json=session.to_backend_payload())
        except httpx.HTTPError as exc:
            raise CodingSessionApiError(f"Lyftix request failed: {exc}") from exc
        if response.status_code != 201:
            raise CodingSessionApiError(f"Lyftix returned HTTP {response.status_code}")
