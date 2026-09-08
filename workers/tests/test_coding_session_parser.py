import json

import pytest

from lyftix_workers.coding_sessions.parser import (
    CodingSessionParseError,
    parse_coding_session_line,
)


def valid_record(**overrides: object) -> str:
    record = {
        "projectName": "lyftix",
        "language": "Python",
        "startedAt": "2026-09-08T09:00:00+01:00",
        "endedAt": "2026-09-08T10:00:00+01:00",
        "source": "editor",
    }
    record.update(overrides)
    return json.dumps(record)


def test_parses_valid_record_and_normalizes_timestamps() -> None:
    record = parse_coding_session_line(valid_record())

    assert record.project_name == "lyftix"
    assert record.started_at == "2026-09-08T08:00:00Z"
    assert record.ended_at == "2026-09-08T09:00:00Z"


def test_preserves_optional_notes() -> None:
    record = parse_coding_session_line(valid_record(notes="Implemented ingestion"))

    assert record.notes == "Implemented ingestion"


def test_uses_default_source_when_source_is_absent() -> None:
    data = json.loads(valid_record())
    del data["source"]

    record = parse_coding_session_line(json.dumps(data), default_source="file")

    assert record.source == "file"


@pytest.mark.parametrize("field", ["projectName", "language", "startedAt", "endedAt", "source"])
def test_rejects_missing_required_fields(field: str) -> None:
    data = json.loads(valid_record())
    del data[field]

    with pytest.raises(CodingSessionParseError):
        parse_coding_session_line(json.dumps(data))


def test_rejects_malformed_timestamp() -> None:
    with pytest.raises(CodingSessionParseError, match="startedAt must be a valid ISO-8601"):
        parse_coding_session_line(valid_record(startedAt="not-a-date"))


@pytest.mark.parametrize(
    "ended_at",
    ["2026-09-08T07:59:59Z", "2026-09-08T08:00:00Z"],
)
def test_rejects_end_before_or_equal_to_start(ended_at: str) -> None:
    with pytest.raises(CodingSessionParseError, match="endedAt must be after startedAt"):
        parse_coding_session_line(valid_record(endedAt=ended_at))


def test_rejects_malformed_json() -> None:
    with pytest.raises(CodingSessionParseError, match="malformed JSON"):
        parse_coding_session_line("{broken")
