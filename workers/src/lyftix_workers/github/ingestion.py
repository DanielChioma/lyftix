from __future__ import annotations

import logging
from dataclasses import dataclass

from lyftix_workers.github.client import GitHubClient
from lyftix_workers.github.mapper import map_github_event
from lyftix_workers.lyftix_client import IngestionOutcome, LyftixApiError, LyftixClient
from lyftix_workers.state import CheckpointStore, GitHubCheckpoint

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
    def __init__(
        self,
        github_client: GitHubClient,
        lyftix_client: LyftixClient,
        checkpoint_store: CheckpointStore | None = None,
    ) -> None:
        self._github_client = github_client
        self._lyftix_client = lyftix_client
        self._checkpoint_store = checkpoint_store

    def run(self) -> IngestionSummary:
        LOGGER.info("GitHub ingestion starting")
        checkpoint = self._checkpoint_store.load() if self._checkpoint_store is not None else None
        if checkpoint is None:
            LOGGER.info("No GitHub checkpoint found; starting from newest event")
        else:
            LOGGER.info(
                "Loaded GitHub checkpoint eventId=%s occurredAt=%s",
                checkpoint.event_id,
                checkpoint.occurred_at,
            )
        if self._checkpoint_store is None:
            events = self._github_client.fetch_recent_events()
            checkpoint_reached = history_exhausted = True
        else:
            fetch_result = self._github_client.fetch_since(
                checkpoint.event_id if checkpoint is not None else None
            )
            events = fetch_result.events
            checkpoint_reached = fetch_result.checkpoint_reached
            history_exhausted = fetch_result.history_exhausted
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
        self._advance_checkpoint_if_safe(
            events,
            summary,
            had_checkpoint=checkpoint is not None,
            checkpoint_reached=checkpoint_reached,
            history_exhausted=history_exhausted,
        )
        LOGGER.info("GitHub ingestion complete: %s", summary)
        return summary

    def _advance_checkpoint_if_safe(
        self,
        events: list[dict],
        summary: IngestionSummary,
        *,
        had_checkpoint: bool,
        checkpoint_reached: bool,
        history_exhausted: bool,
    ) -> None:
        if self._checkpoint_store is None or not events:
            return
        if summary.failed:
            LOGGER.warning("Checkpoint unchanged because %d event(s) failed", summary.failed)
            return
        if had_checkpoint and not (checkpoint_reached or history_exhausted):
            LOGGER.warning(
                "Checkpoint unchanged because pagination stopped before prior checkpoint"
            )
            return
        newest = events[0]
        event_id = newest.get("id")
        occurred_at = newest.get("created_at")
        if not isinstance(event_id, str) or not isinstance(occurred_at, str):
            LOGGER.warning("Checkpoint unchanged because newest event lacks stable identity")
            return
        checkpoint = GitHubCheckpoint(event_id=event_id, occurred_at=occurred_at)
        self._checkpoint_store.save(checkpoint)
        LOGGER.info(
            "Advanced GitHub checkpoint eventId=%s occurredAt=%s",
            checkpoint.event_id,
            checkpoint.occurred_at,
        )
