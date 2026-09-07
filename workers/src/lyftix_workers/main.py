from __future__ import annotations

import argparse
import logging

import httpx
from pydantic import ValidationError

from lyftix_workers.config import WorkerSettings
from lyftix_workers.github.client import GitHubApiError, GitHubClient
from lyftix_workers.github.ingestion import GitHubIngestionJob
from lyftix_workers.lyftix_client import LyftixClient

LOGGER = logging.getLogger(__name__)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run a one-shot Lyftix ingestion worker")
    parser.add_argument("job", choices=["github"])
    parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

    try:
        settings = WorkerSettings()
    except ValidationError as exc:
        LOGGER.error("Invalid worker configuration: %s", exc)
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
        )
        lyftix_client = LyftixClient(lyftix_http_client, str(settings.lyftix_api_base_url))
        try:
            summary = GitHubIngestionJob(github_client, lyftix_client).run()
        except GitHubApiError as exc:
            LOGGER.error("GitHub ingestion could not fetch events: %s", exc)
            return 1
    return 0 if summary.successful else 1


if __name__ == "__main__":
    raise SystemExit(main())
