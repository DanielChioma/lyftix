import threading
from pathlib import Path
from unittest.mock import Mock

from lyftix_workers.github.client import GitHubApiError
from lyftix_workers.scheduler import GitHubScheduler, RunLock


def test_scheduler_runs_repeatedly_and_uses_interval(tmp_path: Path) -> None:
    run_once = Mock()
    waits: list[float] = []

    def waiter(interval: float) -> bool:
        waits.append(interval)
        return len(waits) == 2

    GitHubScheduler(
        run_once, 300, RunLock(tmp_path / "state.json"), threading.Event(), waiter
    ).run()

    assert run_once.call_count == 2
    assert waits == [300, 300]


def test_scheduler_recovers_from_fetch_failure_and_runs_again(tmp_path: Path) -> None:
    run_once = Mock(side_effect=[GitHubApiError("temporary"), object()])
    waits = iter([False, True])

    GitHubScheduler(
        run_once,
        300,
        RunLock(tmp_path / "state.json"),
        threading.Event(),
        lambda interval: next(waits),
    ).run()

    assert run_once.call_count == 2


def test_shutdown_wait_result_stops_future_runs(tmp_path: Path) -> None:
    run_once = Mock()

    GitHubScheduler(
        run_once,
        300,
        RunLock(tmp_path / "state.json"),
        threading.Event(),
        lambda interval: True,
    ).run()

    run_once.assert_called_once()


def test_overlapping_run_is_skipped(tmp_path: Path) -> None:
    run_once = Mock()
    lock = RunLock(tmp_path / "state.json")
    assert lock.try_acquire()
    try:
        GitHubScheduler(
            run_once, 300, lock, threading.Event(), lambda interval: True
        ).run()
    finally:
        lock.release()

    run_once.assert_not_called()


def test_pre_requested_shutdown_runs_nothing(tmp_path: Path) -> None:
    run_once = Mock()
    stop_event = threading.Event()
    stop_event.set()

    GitHubScheduler(
        run_once, 300, RunLock(tmp_path / "state.json"), stop_event, lambda interval: True
    ).run()

    run_once.assert_not_called()


def test_second_lock_for_same_state_path_cannot_overlap(tmp_path: Path) -> None:
    first = RunLock(tmp_path / "state.json")
    second = RunLock(tmp_path / "state.json")
    assert first.try_acquire()
    try:
        assert not second.try_acquire()
    finally:
        first.release()
