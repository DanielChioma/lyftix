from __future__ import annotations

import httpx

from lyftix_workers.system_metrics.collector import SystemMetricSnapshot


class SystemMetricApiError(RuntimeError):
    """Raised when Lyftix rejects or cannot persist a system metric."""


class SystemMetricClient:
    def __init__(self, http_client: httpx.Client, base_url: str) -> None:
        self._http_client = http_client
        self._url = f"{base_url.rstrip('/')}/api/system-metrics"

    def create(self, snapshot: SystemMetricSnapshot) -> None:
        try:
            response = self._http_client.post(self._url, json=snapshot.to_backend_payload())
        except httpx.HTTPError as exc:
            raise SystemMetricApiError(f"Lyftix request failed: {exc}") from exc
        if response.status_code != 201:
            raise SystemMetricApiError(f"Lyftix returned HTTP {response.status_code}")
