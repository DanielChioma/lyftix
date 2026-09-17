from __future__ import annotations

import argparse
import logging
import signal
import threading
from collections.abc import Callable

import httpx
from pydantic import ValidationError

from lyftix_workers.auth import LyftixAuthenticationError, authenticate
from lyftix_workers.coding_sessions.client import CodingSessionClient
from lyftix_workers.coding_sessions.ingestion import CodingSessionIngestionJob
from lyftix_workers.config import CodingSessionSettings, SystemMetricSettings, WorkerSettings
from lyftix_workers.github.client import GitHubApiError, GitHubClient
from lyftix_workers.github.ingestion import GitHubIngestionJob
from lyftix_workers.lyftix_client import LyftixClient
from lyftix_workers.scheduler import GitHubScheduler, RunLock, SystemMetricScheduler
from lyftix_workers.state import CheckpointError, CheckpointStore
from lyftix_workers.system_metrics.client import SystemMetricApiError, SystemMetricClient
from lyftix_workers.system_metrics.collector import (
    LinuxHostSystemMetricCollector,
    SystemMetricCollectionError,
    SystemMetricCollector,
)
from lyftix_workers.system_metrics.ingestion import SystemMetricIngestionJob

LOGGER = logging.getLogger(__name__)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run Lyftix ingestion workers")
    parser.add_argument(
        "job",
        choices=[
            "github",
            "github-schedule",
            "coding-sessions",
            "system-metrics",
            "system-metrics-schedule",
        ],
    )
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

    if args.job == "coding-sessions":
        return run_coding_sessions()
    if args.job == "system-metrics":
        return run_system_metrics()
    if args.job == "system-metrics-schedule":
        return run_system_metrics(args.job)
    return run_github(args.job)


def run_coding_sessions() -> int:
    try:
        settings = CodingSessionSettings()
    except ValidationError as exc:
        LOGGER.error("Invalid coding-session configuration: %s", exc)
        return 2

    timeout = httpx.Timeout(settings.http_timeout_seconds)
    try:
        with httpx.Client(timeout=timeout) as http_client:
            authenticate(
                http_client,
                str(settings.lyftix_api_base_url),
                settings.lyftix_worker_username,
                settings.lyftix_worker_password.get_secret_value(),
            )
            summary = CodingSessionIngestionJob(
                CodingSessionClient(http_client, str(settings.lyftix_api_base_url)),
                settings.coding_sessions_input_path,
                settings.coding_session_default_source,
            ).run()
    except LyftixAuthenticationError as exc:
        LOGGER.error("Coding-session authentication failed: %s", exc)
        return 1
    return 0 if summary.successful else 1


def run_system_metrics(job: str = "system-metrics") -> int:
    try:
        settings = SystemMetricSettings()
    except ValidationError as exc:
        LOGGER.error("Invalid system-metrics configuration: %s", exc)
        return 2

    timeout = httpx.Timeout(settings.http_timeout_seconds)
    try:
        with httpx.Client(timeout=timeout) as http_client:
            ingestion_job = SystemMetricIngestionJob(
                create_system_metric_collector(settings),
                SystemMetricClient(http_client, str(settings.lyftix_api_base_url)),
            )
            if job == "system-metrics":
                authenticate_worker(http_client, settings)
                ingestion_job.run()
                return 0

            stop_event = threading.Event()
            install_shutdown_handlers(stop_event, "system metrics")
            SystemMetricScheduler(
                lambda: run_authenticated(http_client, settings, ingestion_job.run),
                settings.system_metrics_interval_seconds,
                stop_event,
            ).run()
    except (LyftixAuthenticationError, SystemMetricCollectionError, SystemMetricApiError) as exc:
        LOGGER.error("System metric ingestion failed: %s", exc)
        return 1
    return 0


def create_system_metric_collector(
    settings: SystemMetricSettings,
) -> SystemMetricCollector:
    if settings.system_metrics_collection_mode == "host":
        return LinuxHostSystemMetricCollector(
            settings.system_metrics_hostname or "",
            settings.system_metrics_source,
            settings.system_metrics_proc_path,
            settings.system_metrics_disk_path,
        )
    return SystemMetricCollector(
        settings.system_metrics_source,
        settings.system_metrics_disk_path,
    )


def run_github(job: str) -> int:
    try:
        settings = WorkerSettings()
    except ValidationError as exc:
        LOGGER.error("Invalid worker configuration: %s", exc)
        return 2

    checkpoint_store = CheckpointStore(settings.worker_state_path)
    try:
        checkpoint_store.validate_path()
        checkpoint_store.load()
    except CheckpointError as exc:
        LOGGER.error("Invalid worker state: %s", exc)
        return 2

    timeout = httpx.Timeout(settings.http_timeout_seconds)
    try:
        with httpx.Client(timeout=timeout) as github_http_client, httpx.Client(
            timeout=timeout
        ) as lyftix_http_client:
            github_client = GitHubClient(
                github_http_client,
                str(settings.github_api_base_url),
                settings.github_token.get_secret_value(),
                settings.github_username,
                settings.github_events_per_page,
                settings.github_max_pages_per_run,
            )
            lyftix_client = LyftixClient(lyftix_http_client, str(settings.lyftix_api_base_url))
            ingestion_job = GitHubIngestionJob(github_client, lyftix_client, checkpoint_store)
            run_lock = RunLock(settings.worker_state_path)
            if job == "github":
                authenticate_worker(lyftix_http_client, settings)
                with run_lock:
                    summary = ingestion_job.run()
                return 0 if summary.successful else 1

            stop_event = threading.Event()
            install_shutdown_handlers(stop_event, "GitHub")
            GitHubScheduler(
                lambda: run_authenticated(
                    lyftix_http_client,
                    settings,
                    ingestion_job.run,
                ),
                settings.github_ingestion_interval_seconds,
                run_lock,
                stop_event,
            ).run()
            return 0
    except GitHubApiError as exc:
        LOGGER.error("GitHub ingestion could not fetch events: %s", exc)
        return 1
    except LyftixAuthenticationError as exc:
        LOGGER.error("GitHub worker authentication failed: %s", exc)
        return 1
    except RuntimeError as exc:
        LOGGER.error("Cannot start GitHub ingestion: %s", exc)
        return 1


def install_shutdown_handlers(stop_event: threading.Event, scheduler_name: str) -> None:
    def request_shutdown(signum: int, frame: object) -> None:
        del frame
        LOGGER.info("Received signal %d; stopping %s scheduler", signum, scheduler_name)
        stop_event.set()

    signal.signal(signal.SIGINT, request_shutdown)
    signal.signal(signal.SIGTERM, request_shutdown)


def authenticate_worker(
    http_client: httpx.Client,
    settings: WorkerSettings | CodingSessionSettings | SystemMetricSettings,
) -> None:
    authenticate(
        http_client,
        str(settings.lyftix_api_base_url),
        settings.lyftix_worker_username,
        settings.lyftix_worker_password.get_secret_value(),
    )


def run_authenticated(
    http_client: httpx.Client,
    settings: WorkerSettings | SystemMetricSettings,
    run_once: Callable[[], object],
) -> object:
    authenticate_worker(http_client, settings)
    return run_once()


if __name__ == "__main__":
    raise SystemExit(main())
