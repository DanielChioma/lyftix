import json
import sys
from pathlib import Path

import httpx
import pytest
from pydantic import ValidationError

import lyftix_workers.main as main_module
from lyftix_workers.config import SystemMetricSettings
from lyftix_workers.system_metrics.collector import SystemMetricSnapshot
from lyftix_workers.system_metrics.ingestion import SystemMetricIngestionJob


class StubCollector:
    def collect(self) -> SystemMetricSnapshot:
        return metric_snapshot()


class StubClient:
    def __init__(self) -> None:
        self.received: list[SystemMetricSnapshot] = []

    def create(self, snapshot: SystemMetricSnapshot) -> None:
        self.received.append(snapshot)


def metric_snapshot() -> SystemMetricSnapshot:
    return SystemMetricSnapshot(
        "lyftix-host", "local", 37.5, 400, 1000, 500, 2000, None,
        "2026-09-08T12:00:00Z",
    )


def clear_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in ("LYFTIX_API_BASE_URL", "SYSTEM_METRICS_SOURCE", "SYSTEM_METRICS_DISK_PATH"):
        monkeypatch.delenv(name, raising=False)


def test_job_collects_and_posts_snapshot() -> None:
    client = StubClient()

    SystemMetricIngestionJob(StubCollector(), client).run()

    assert client.received == [metric_snapshot()]


def test_configuration_failure_for_missing_api_url(monkeypatch: pytest.MonkeyPatch) -> None:
    clear_environment(monkeypatch)

    with pytest.raises(ValidationError, match="LYFTIX_API_BASE_URL"):
        SystemMetricSettings()


def test_configuration_rejects_invalid_disk_path(
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
) -> None:
    clear_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")
    monkeypatch.setenv("SYSTEM_METRICS_DISK_PATH", str(tmp_path / "missing"))

    with pytest.raises(ValidationError, match="existing directory"):
        SystemMetricSettings()


def test_cli_routes_system_metrics_command(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(sys, "argv", ["lyftix-worker", "system-metrics"])
    monkeypatch.setattr(main_module, "run_system_metrics", lambda: 7)

    assert main_module.main() == 7


@pytest.mark.parametrize(("status_code", "expected_exit"), [(201, 0), (500, 1)])
def test_command_posts_exact_payload_and_returns_exit_code(
    monkeypatch: pytest.MonkeyPatch,
    status_code: int,
    expected_exit: int,
) -> None:
    clear_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://lyftix.test")
    monkeypatch.setattr(main_module, "SystemMetricCollector", lambda source, path: StubCollector())
    posted: list[dict] = []

    def handler(request: httpx.Request) -> httpx.Response:
        posted.append(json.loads(request.content))
        return httpx.Response(status_code, request=request)

    client_type = httpx.Client
    monkeypatch.setattr(
        main_module.httpx,
        "Client",
        lambda **kwargs: client_type(transport=httpx.MockTransport(handler), **kwargs),
    )

    assert main_module.run_system_metrics() == expected_exit
    assert posted == [metric_snapshot().to_backend_payload()]
