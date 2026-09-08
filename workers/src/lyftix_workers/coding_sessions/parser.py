from __future__ import annotations

import json
from collections.abc import Iterator
from dataclasses import dataclass
from datetime import UTC, datetime
from json import JSONDecodeError
from pathlib import Path
from typing import Any


class CodingSessionParseError(ValueError):
    """Raised when one JSONL record cannot be converted to a coding session."""


@dataclass(frozen=True)
class CodingSessionRecord:
    project_name: str
    language: str
    started_at: str
    ended_at: str
    source: str
    notes: str | None = None

    def to_backend_payload(self) -> dict[str, str | None]:
        return {
            "projectName": self.project_name,
            "language": self.language,
            "startedAt": self.started_at,
            "endedAt": self.ended_at,
            "source": self.source,
            "notes": self.notes,
        }


@dataclass(frozen=True)
class CodingSessionParseResult:
    line_number: int
    record: CodingSessionRecord | None = None
    error: str | None = None


def parse_coding_session_line(
    line: str,
    *,
    default_source: str | None = None,
) -> CodingSessionRecord:
    try:
        raw = json.loads(line)
    except JSONDecodeError as exc:
        raise CodingSessionParseError("malformed JSON") from exc
    if not isinstance(raw, dict):
        raise CodingSessionParseError("record must be a JSON object")

    project_name = _required_string(raw, "projectName", 255)
    language = _required_string(raw, "language", 100)
    source_value = raw.get("source", default_source)
    source = _validate_string(source_value, "source", 100, required=True)
    notes_value = raw.get("notes")
    notes = _validate_string(notes_value, "notes", 5000, required=False)
    started_at, started_value = _timestamp(raw, "startedAt")
    ended_at, ended_value = _timestamp(raw, "endedAt")
    if ended_value <= started_value:
        raise CodingSessionParseError("endedAt must be after startedAt")

    return CodingSessionRecord(
        project_name=project_name,
        language=language,
        started_at=_canonical_timestamp(started_at),
        ended_at=_canonical_timestamp(ended_at),
        source=source,
        notes=notes,
    )


def parse_coding_session_file(
    path: Path,
    *,
    default_source: str | None = None,
) -> Iterator[CodingSessionParseResult]:
    with path.open(encoding="utf-8") as input_file:
        for line_number, line in enumerate(input_file, start=1):
            if not line.strip():
                continue
            try:
                yield CodingSessionParseResult(
                    line_number=line_number,
                    record=parse_coding_session_line(line, default_source=default_source),
                )
            except CodingSessionParseError as exc:
                yield CodingSessionParseResult(line_number=line_number, error=str(exc))


def _required_string(raw: dict[str, Any], name: str, maximum: int) -> str:
    if name not in raw:
        raise CodingSessionParseError(f"missing required field: {name}")
    value = _validate_string(raw[name], name, maximum, required=True)
    assert value is not None
    return value


def _validate_string(
    value: Any,
    name: str,
    maximum: int,
    *,
    required: bool,
) -> str | None:
    if value is None and not required:
        return None
    if not isinstance(value, str) or not value.strip():
        raise CodingSessionParseError(f"{name} must be a nonblank string")
    if len(value) > maximum:
        raise CodingSessionParseError(f"{name} must contain at most {maximum} characters")
    return value


def _timestamp(raw: dict[str, Any], name: str) -> tuple[datetime, datetime]:
    value = _required_string(raw, name, 100)
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise CodingSessionParseError(f"{name} must be a valid ISO-8601 timestamp") from exc
    if parsed.tzinfo is None:
        raise CodingSessionParseError(f"{name} must include a UTC offset")
    return parsed, parsed.astimezone(UTC)


def _canonical_timestamp(value: datetime) -> str:
    return value.astimezone(UTC).isoformat().replace("+00:00", "Z")
