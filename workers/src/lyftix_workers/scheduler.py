from __future__ import annotations

import fcntl
import logging
import threading
from collections.abc import Callable
from pathlib import Path
from types import TracebackType

from lyftix_workers.github.client import GitHubApiError

LOGGER = logging.getLogger(__name__)


class RunLock:
    """Non-blocking thread and process lock associated with one state file."""

    def __init__(self, state_path: Path) -> None:
        self._thread_lock = threading.Lock()
        self._lock_path = state_path.expanduser().with_suffix(state_path.suffix + ".lock")
        self._file = None

    def try_acquire(self) -> bool:
        if not self._thread_lock.acquire(blocking=False):
            return False
        try:
            self._lock_path.parent.mkdir(parents=True, exist_ok=True)
            self._file = self._lock_path.open("a+")
            fcntl.flock(self._file.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except (OSError, BlockingIOError):
            if self._file is not None:
                self._file.close()
                self._file = None
            self._thread_lock.release()
            return False
        return True

    def release(self) -> None:
        if self._file is not None:
            fcntl.flock(self._file.fileno(), fcntl.LOCK_UN)
            self._file.close()
            self._file = None
        self._thread_lock.release()

    def __enter__(self) -> RunLock:
        if not self.try_acquire():
            raise RuntimeError("GitHub ingestion is already running for this state path")
        return self

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        self.release()


class GitHubScheduler:
    def __init__(
        self,
        run_once: Callable[[], object],
        interval_seconds: int,
        run_lock: RunLock,
        stop_event: threading.Event,
        waiter: Callable[[float], bool] | None = None,
    ) -> None:
        self._run_once = run_once
        self._interval_seconds = interval_seconds
        self._run_lock = run_lock
        self._stop_event = stop_event
        self._waiter = waiter or stop_event.wait

    def run(self) -> None:
        LOGGER.info("GitHub scheduler starting intervalSeconds=%d", self._interval_seconds)
        while not self._stop_event.is_set():
            if not self._run_lock.try_acquire():
                LOGGER.warning(
                    "Skipping scheduled pass because GitHub ingestion is already running"
                )
            else:
                try:
                    result = self._run_once()
                    if getattr(result, "successful", True) is False:
                        LOGGER.error("Scheduled GitHub ingestion completed with event failures")
                except GitHubApiError as exc:
                    LOGGER.error("Scheduled GitHub ingestion failed and will retry: %s", exc)
                finally:
                    self._run_lock.release()
            if self._waiter(self._interval_seconds):
                break
        LOGGER.info("GitHub scheduler stopped")
