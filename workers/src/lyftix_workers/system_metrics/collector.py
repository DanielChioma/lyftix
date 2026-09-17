from __future__ import annotations

import os
import socket
import time
from collections.abc import Callable
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path

import psutil


class SystemMetricCollectionError(RuntimeError):
    """Raised when the host does not provide a valid metric snapshot."""


@dataclass(frozen=True)
class SystemMetricSnapshot:
    hostname: str
    source: str
    cpu_percent: float
    memory_used_bytes: int
    memory_total_bytes: int
    disk_used_bytes: int
    disk_total_bytes: int
    load_average_1m: float | None
    uptime_seconds: int
    collected_at: str

    def to_backend_payload(self) -> dict[str, str | float | int | None]:
        return {
            "hostname": self.hostname,
            "source": self.source,
            "cpuPercent": self.cpu_percent,
            "memoryUsedBytes": self.memory_used_bytes,
            "memoryTotalBytes": self.memory_total_bytes,
            "diskUsedBytes": self.disk_used_bytes,
            "diskTotalBytes": self.disk_total_bytes,
            "loadAverage1m": self.load_average_1m,
            "uptimeSeconds": self.uptime_seconds,
            "collectedAt": self.collected_at,
        }


class SystemMetricCollector:
    def __init__(
        self,
        source: str,
        disk_path: Path,
        clock: Callable[[], datetime] | None = None,
    ) -> None:
        self._source = source
        self._disk_path = disk_path
        self._clock = clock or (lambda: datetime.now(UTC))

    def collect(self) -> SystemMetricSnapshot:
        collected_at = self._clock()
        hostname = socket.gethostname()
        memory = psutil.virtual_memory()
        disk = psutil.disk_usage(str(self._disk_path))
        snapshot = SystemMetricSnapshot(
            hostname=hostname,
            source=self._source,
            cpu_percent=float(psutil.cpu_percent(interval=None)),
            memory_used_bytes=int(memory.used),
            memory_total_bytes=int(memory.total),
            disk_used_bytes=int(disk.used),
            disk_total_bytes=int(disk.total),
            load_average_1m=self._load_average_1m(),
            uptime_seconds=max(0, int(collected_at.timestamp() - psutil.boot_time())),
            collected_at=collected_at.astimezone(UTC).isoformat().replace("+00:00", "Z"),
        )
        self._validate(snapshot)
        return snapshot

    def _load_average_1m(self) -> float | None:
        try:
            return float(os.getloadavg()[0])
        except (AttributeError, OSError):
            return None

    def _validate(self, snapshot: SystemMetricSnapshot) -> None:
        if not snapshot.hostname.strip() or not snapshot.source.strip():
            raise SystemMetricCollectionError("hostname and source must not be blank")
        if not 0 <= snapshot.cpu_percent <= 100:
            raise SystemMetricCollectionError("cpuPercent must be between 0 and 100")
        if (
            snapshot.memory_total_bytes <= 0
            or not 0 <= snapshot.memory_used_bytes <= snapshot.memory_total_bytes
        ):
            raise SystemMetricCollectionError("invalid memory byte values")
        if (
            snapshot.disk_total_bytes <= 0
            or not 0 <= snapshot.disk_used_bytes <= snapshot.disk_total_bytes
        ):
            raise SystemMetricCollectionError("invalid disk byte values")
        if snapshot.load_average_1m is not None and snapshot.load_average_1m < 0:
            raise SystemMetricCollectionError("loadAverage1m must not be negative")
        if snapshot.uptime_seconds < 0:
            raise SystemMetricCollectionError("uptimeSeconds must not be negative")


@dataclass(frozen=True)
class CpuTimes:
    total: int
    idle: int


class LinuxHostSystemMetricCollector(SystemMetricCollector):
    def __init__(
        self,
        hostname: str,
        source: str,
        proc_path: Path,
        disk_path: Path,
        clock: Callable[[], datetime] | None = None,
        sleeper: Callable[[float], None] = time.sleep,
        initial_cpu_sample_seconds: float = 0.1,
    ) -> None:
        super().__init__(source, disk_path, clock)
        self._hostname = hostname
        self._proc_path = proc_path
        self._sleeper = sleeper
        self._initial_cpu_sample_seconds = initial_cpu_sample_seconds
        self._previous_cpu_times: CpuTimes | None = None

    def collect(self) -> SystemMetricSnapshot:
        start_cpu_times = self._previous_cpu_times or self._read_cpu_times()
        if self._previous_cpu_times is None:
            self._sleeper(self._initial_cpu_sample_seconds)
        end_cpu_times = self._read_cpu_times()
        self._previous_cpu_times = end_cpu_times

        memory_used, memory_total = self._read_memory()
        disk = psutil.disk_usage(str(self._disk_path))
        snapshot = SystemMetricSnapshot(
            hostname=self._hostname,
            source=self._source,
            cpu_percent=self._cpu_percent(start_cpu_times, end_cpu_times),
            memory_used_bytes=memory_used,
            memory_total_bytes=memory_total,
            disk_used_bytes=int(disk.used),
            disk_total_bytes=int(disk.total),
            load_average_1m=self._read_float("loadavg", 0),
            uptime_seconds=int(self._read_float("uptime", 0)),
            collected_at=self._clock().astimezone(UTC).isoformat().replace("+00:00", "Z"),
        )
        self._validate(snapshot)
        return snapshot

    def _read_cpu_times(self) -> CpuTimes:
        fields = self._read_text("stat").splitlines()[0].split()
        if not fields or fields[0] != "cpu" or len(fields) < 5:
            raise SystemMetricCollectionError("host proc stat does not contain aggregate CPU data")
        try:
            values = [int(value) for value in fields[1:9]]
        except ValueError as exc:
            raise SystemMetricCollectionError("host proc stat contains invalid CPU data") from exc
        return CpuTimes(total=sum(values), idle=values[3] + (values[4] if len(values) > 4 else 0))

    def _read_memory(self) -> tuple[int, int]:
        values: dict[str, int] = {}
        for line in self._read_text("meminfo").splitlines():
            name, separator, raw_value = line.partition(":")
            if separator and raw_value.strip():
                try:
                    values[name] = int(raw_value.split()[0]) * 1024
                except ValueError as exc:
                    raise SystemMetricCollectionError("host proc meminfo is invalid") from exc
        try:
            total = values["MemTotal"]
            available = values["MemAvailable"]
        except KeyError as exc:
            raise SystemMetricCollectionError(
                "host proc meminfo must contain MemTotal and MemAvailable"
            ) from exc
        return total - available, total

    def _read_float(self, filename: str, field: int) -> float:
        try:
            return float(self._read_text(filename).split()[field])
        except (IndexError, ValueError) as exc:
            raise SystemMetricCollectionError(f"host proc {filename} is invalid") from exc

    def _read_text(self, filename: str) -> str:
        try:
            return (self._proc_path / filename).read_text(encoding="utf-8")
        except OSError as exc:
            raise SystemMetricCollectionError(f"cannot read host proc {filename}: {exc}") from exc

    @staticmethod
    def _cpu_percent(start: CpuTimes, end: CpuTimes) -> float:
        total_delta = end.total - start.total
        idle_delta = end.idle - start.idle
        if total_delta <= 0 or idle_delta < 0:
            raise SystemMetricCollectionError("host CPU counters did not increase")
        return min(100.0, max(0.0, 100.0 * (total_delta - idle_delta) / total_delta))
