from __future__ import annotations

import argparse
import logging
import signal
import threading

import httpx
from pydantic import ValidationError

from lyftix_workers.coding_sessions.client import CodingSessionClient
from lyftix_workers.coding_sessions.ingestion import CodingSessionIngestionJob
from lyftix_workers.config import CodingSessionSettings, WorkerSettings
from lyftix_workers.github.client import GitHubApiError, GitHubClient
from lyftix_workers.github.ingestion import GitHubIngestionJob
from lyftix_workers.lyftix_client import LyftixClient
from lyftix_workers.scheduler import GitHubScheduler, RunLock
from lyftix_workers.state import CheckpointError, CheckpointStore

LOGGER = logging.getLogger(__name__)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run Lyftix ingestion workers")
    parser.add_argument("job", choices=["github", "github-schedule", "coding-sessions"])
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

    if args.job == "coding-sessions":
        return run_coding_sessions()
    return run_github(args.job)


def run_coding_sessions() -> int:
    try:
        settings = CodingSessionSettings()
    except ValidationError as exc:
        LOGGER.error("Invalid coding-session configuration: %s", exc)
        return 2

    timeout = httpx.Timeout(settings.http_timeout_seconds)
    with httpx.Client(timeout=timeout) as http_client:
        summary = CodingSessionIngestionJob(
            CodingSessionClient(http_client, str(settings.lyftix_api_base_url)),
            settings.coding_sessions_input_path,
            settings.coding_session_default_source,
        ).run()
    return 0 if summary.successful else 1


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
            try:
                with run_lock:
                    summary = ingestion_job.run()
            except GitHubApiError as exc:
                LOGGER.error("GitHub ingestion could not fetch events: %s", exc)
                return 1
            except RuntimeError as exc:
                LOGGER.error("Cannot start GitHub ingestion: %s", exc)
                return 1
            return 0 if summary.successful else 1

        stop_event = threading.Event()

        def request_shutdown(signum: int, frame: object) -> None:
            del frame
            LOGGER.info("Received signal %d; stopping GitHub scheduler", signum)
            stop_event.set()

        signal.signal(signal.SIGINT, request_shutdown)
        signal.signal(signal.SIGTERM, request_shutdown)
        GitHubScheduler(
            ingestion_job.run,
            settings.github_ingestion_interval_seconds,
            run_lock,
            stop_event,
        ).run()
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
