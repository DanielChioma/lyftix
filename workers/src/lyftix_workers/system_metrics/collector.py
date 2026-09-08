from __future__ import annotations

import os
import socket
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
            collected_at=self._clock().astimezone(UTC).isoformat().replace("+00:00", "Z"),
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
