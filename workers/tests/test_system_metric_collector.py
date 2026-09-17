from datetime import UTC, datetime
from pathlib import Path
from types import SimpleNamespace

import pytest

import lyftix_workers.system_metrics.collector as collector_module
from lyftix_workers.system_metrics.collector import (
    LinuxHostSystemMetricCollector,
    SystemMetricCollectionError,
    SystemMetricCollector,
)


@pytest.fixture
def metric_boundaries(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(collector_module.socket, "gethostname", lambda: "lyftix-host")
    monkeypatch.setattr(collector_module.psutil, "cpu_percent", lambda interval: 37.5)
    monkeypatch.setattr(
        collector_module.psutil,
        "boot_time",
        lambda: datetime(2026, 9, 8, 11, 0, tzinfo=UTC).timestamp(),
    )
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
    assert snapshot.uptime_seconds == 3600
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


def test_collects_explicit_linux_host_metrics_from_narrow_inputs(
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
) -> None:
    proc_path = tmp_path / "proc"
    proc_path.mkdir()
    (proc_path / "stat").write_text("cpu 100 0 50 850 0 0 0 0 0 0\n", encoding="utf-8")
    (proc_path / "meminfo").write_text(
        "MemTotal: 1000 kB\nMemAvailable: 400 kB\n",
        encoding="utf-8",
    )
    (proc_path / "loadavg").write_text("1.75 1.50 1.25 1/100 123\n", encoding="utf-8")
    (proc_path / "uptime").write_text("12345.67 456.00\n", encoding="utf-8")
    disk_path = tmp_path / "host-disk"
    disk_path.mkdir()
    measured_paths: list[str] = []

    def disk_usage(path: str) -> SimpleNamespace:
        measured_paths.append(path)
        return SimpleNamespace(used=600, total=2400)

    def advance_cpu(_: float) -> None:
        (proc_path / "stat").write_text(
            "cpu 150 0 70 880 0 0 0 0 0 0\n",
            encoding="utf-8",
        )

    monkeypatch.setattr(collector_module.psutil, "disk_usage", disk_usage)

    snapshot = LinuxHostSystemMetricCollector(
        "ds-server",
        "host-worker",
        proc_path,
        disk_path,
        clock=lambda: datetime(2026, 9, 8, 12, 0, tzinfo=UTC),
        sleeper=advance_cpu,
    ).collect()

    assert snapshot.hostname == "ds-server"
    assert snapshot.source == "host-worker"
    assert snapshot.cpu_percent == 70.0
    assert (snapshot.memory_used_bytes, snapshot.memory_total_bytes) == (614400, 1024000)
    assert (snapshot.disk_used_bytes, snapshot.disk_total_bytes) == (600, 2400)
    assert measured_paths == [str(disk_path)]
    assert snapshot.load_average_1m == 1.75
    assert snapshot.uptime_seconds == 12345
    assert snapshot.collected_at == "2026-09-08T12:00:00Z"


def test_host_cpu_uses_delta_from_previous_sample(
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
) -> None:
    proc_path = tmp_path / "proc"
    proc_path.mkdir()
    for name, content in {
        "stat": "cpu 10 0 10 80 0 0 0 0\n",
        "meminfo": "MemTotal: 100 kB\nMemAvailable: 50 kB\n",
        "loadavg": "0.5 0.4 0.3 1/1 1\n",
        "uptime": "10.0 2.0\n",
    }.items():
        (proc_path / name).write_text(content, encoding="utf-8")
    disk_path = tmp_path / "disk"
    disk_path.mkdir()
    monkeypatch.setattr(
        collector_module.psutil,
        "disk_usage",
        lambda path: SimpleNamespace(used=1, total=2),
    )

    def initial_sample(_: float) -> None:
        (proc_path / "stat").write_text("cpu 20 0 10 90 0 0 0 0\n", encoding="utf-8")

    collector = LinuxHostSystemMetricCollector(
        "ds-server", "host-worker", proc_path, disk_path, sleeper=initial_sample
    )
    assert collector.collect().cpu_percent == 50.0

    (proc_path / "stat").write_text("cpu 30 0 20 100 0 0 0 0\n", encoding="utf-8")
    assert collector.collect().cpu_percent == pytest.approx(66.6666667)


def test_host_collector_reports_missing_proc_input(tmp_path: Path) -> None:
    proc_path = tmp_path / "proc"
    proc_path.mkdir()
    with pytest.raises(SystemMetricCollectionError, match="cannot read host proc stat"):
        LinuxHostSystemMetricCollector(
            "ds-server", "host-worker", proc_path, tmp_path, sleeper=lambda _: None
        ).collect()
