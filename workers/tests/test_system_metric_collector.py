from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace

import pytest

import lyftix_workers.system_metrics.collector as collector_module
from lyftix_workers.system_metrics.collector import SystemMetricCollector


@pytest.fixture
def metric_boundaries(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(collector_module.socket, "gethostname", lambda: "lyftix-host")
    monkeypatch.setattr(collector_module.psutil, "cpu_percent", lambda interval: 37.5)
    monkeypatch.setattr(
        collector_module.psutil,
        "virtual_memory",
        lambda: SimpleNamespace(used=400, total=1000),
    )
    monkeypatch.setattr(
        collector_module.psutil,
        "disk_usage",
        lambda path: SimpleNamespace(used=500, total=2000),
    )


def test_collects_host_cpu_memory_disk_time_and_load_average(
    monkeypatch: pytest.MonkeyPatch,
    metric_boundaries: None,
) -> None:
    monkeypatch.setattr(collector_module.os, "getloadavg", lambda: (1.25, 1.0, 0.75))

    snapshot = SystemMetricCollector(
        "local",
        Path("/metrics"),
        clock=lambda: datetime(2026, 9, 8, 12, 0, tzinfo=UTC),
    ).collect()

    assert snapshot.hostname == "lyftix-host"
    assert snapshot.source == "local"
    assert snapshot.cpu_percent == 37.5
    assert (snapshot.memory_used_bytes, snapshot.memory_total_bytes) == (400, 1000)
    assert (snapshot.disk_used_bytes, snapshot.disk_total_bytes) == (500, 2000)
    assert snapshot.load_average_1m == 1.25
    assert snapshot.collected_at == "2026-09-08T12:00:00Z"


def test_sets_load_average_to_none_when_unsupported(
    monkeypatch: pytest.MonkeyPatch,
    metric_boundaries: None,
) -> None:
    def unsupported() -> tuple[float, float, float]:
        raise OSError("unsupported")

    monkeypatch.setattr(collector_module.os, "getloadavg", unsupported)

    snapshot = SystemMetricCollector(
        "local",
        Path("/metrics"),
        clock=lambda: datetime(2026, 9, 8, 12, 0, tzinfo=UTC),
    ).collect()

    assert snapshot.load_average_1m is None
