from __future__ import annotations

import logging
from dataclasses import dataclass
from pathlib import Path

from lyftix_workers.coding_sessions.client import CodingSessionApiError, CodingSessionClient
from lyftix_workers.coding_sessions.parser import parse_coding_session_file

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class CodingSessionIngestionSummary:
    read: int = 0
    valid: int = 0
    created: int = 0
    failed: int = 0

    @property
    def successful(self) -> bool:
        return self.failed == 0


class CodingSessionIngestionJob:
    def __init__(
        self,
        client: CodingSessionClient,
        input_path: Path,
        default_source: str | None = None,
    ) -> None:
        self._client = client
        self._input_path = input_path
        self._default_source = default_source

    def run(self) -> CodingSessionIngestionSummary:
        LOGGER.info("Starting coding-session ingestion from %s", self._input_path)
        read = valid = created = failed = 0
        for result in parse_coding_session_file(
            self._input_path,
            default_source=self._default_source,
        ):
            read += 1
            if result.error is not None:
                failed += 1
                LOGGER.error(
                    "Coding-session parse failed at line %d: %s",
                    result.line_number,
                    result.error,
                )
                continue

            valid += 1
            assert result.record is not None
            try:
                self._client.create(result.record)
            except CodingSessionApiError as exc:
                failed += 1
                LOGGER.error(
                    "Coding-session submission failed at line %d: %s",
                    result.line_number,
                    exc,
                )
                continue
            created += 1
            LOGGER.info("Created coding session from line %d", result.line_number)

        summary = CodingSessionIngestionSummary(read, valid, created, failed)
        LOGGER.info(
            "Coding-session ingestion complete: read=%d valid=%d created=%d failed=%d",
            summary.read,
            summary.valid,
            summary.created,
            summary.failed,
        )
        return summary
