from __future__ import annotations

import logging
from dataclasses import dataclass

from lyftix_workers.github.client import GitHubClient
from lyftix_workers.github.mapper import map_github_event
from lyftix_workers.lyftix_client import IngestionOutcome, LyftixApiError, LyftixClient

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class IngestionSummary:
    fetched: int = 0
    mapped: int = 0
    created: int = 0
    duplicates: int = 0
    skipped: int = 0
    failed: int = 0

    @property
    def successful(self) -> bool:
        return self.failed == 0


class GitHubIngestionJob:
    def __init__(self, github_client: GitHubClient, lyftix_client: LyftixClient) -> None:
        self._github_client = github_client
        self._lyftix_client = lyftix_client

    def run(self) -> IngestionSummary:
        LOGGER.info("GitHub ingestion starting")
        events = self._github_client.fetch_recent_events()
        LOGGER.info("Fetched %d GitHub events", len(events))
        mapped = created = duplicates = skipped = failed = 0

        for event in events:
            activity = map_github_event(event)
            if activity is None:
                skipped += 1
                LOGGER.info(
                    "Skipping unsupported or insufficient GitHub event id=%s type=%s",
                    event.get("id", "unknown"),
                    event.get("type", "unknown"),
                )
                continue
            mapped += 1
            try:
                outcome = self._lyftix_client.create_github_activity(activity)
            except LyftixApiError as exc:
                failed += 1
                LOGGER.error(
                    "Failed to ingest GitHub event externalId=%s: %s",
                    activity.external_id,
                    exc,
                )
                continue
            if outcome is IngestionOutcome.CREATED:
                created += 1
                LOGGER.info("Created GitHub activity externalId=%s", activity.external_id)
            else:
                duplicates += 1
                LOGGER.info("GitHub activity already ingested externalId=%s", activity.external_id)

        summary = IngestionSummary(
            fetched=len(events),
            mapped=mapped,
            created=created,
            duplicates=duplicates,
            skipped=skipped,
            failed=failed,
        )
        LOGGER.info("GitHub ingestion complete: %s", summary)
        return summary
