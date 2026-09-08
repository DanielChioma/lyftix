import json
from pathlib import Path

from lyftix_workers.coding_sessions.client import CodingSessionApiError
from lyftix_workers.coding_sessions.ingestion import CodingSessionIngestionJob
from lyftix_workers.coding_sessions.parser import CodingSessionRecord


class StubClient:
    def __init__(self, failing_projects: set[str] | None = None) -> None:
        self.created: list[CodingSessionRecord] = []
        self._failing_projects = failing_projects or set()

    def create(self, session: CodingSessionRecord) -> None:
        if session.project_name in self._failing_projects:
            raise CodingSessionApiError("Lyftix returned HTTP 500")
        self.created.append(session)


def record(project_name: str) -> str:
    return json.dumps(
        {
            "projectName": project_name,
            "language": "Python",
            "startedAt": "2026-09-08T08:00:00Z",
            "endedAt": "2026-09-08T09:00:00Z",
            "source": "file",
        }
    )


def input_file(tmp_path: Path, lines: list[str]) -> Path:
    path = tmp_path / "sessions.jsonl"
    path.write_text("\n".join(lines), encoding="utf-8")
    return path


def test_all_valid_records_are_created(tmp_path: Path) -> None:
    client = StubClient()

    summary = CodingSessionIngestionJob(
        client, input_file(tmp_path, [record("one"), record("two")])
    ).run()

    assert (summary.read, summary.valid, summary.created, summary.failed) == (2, 2, 2, 0)
    assert summary.successful


def test_malformed_record_does_not_stop_later_records(tmp_path: Path) -> None:
    client = StubClient()

    summary = CodingSessionIngestionJob(
        client, input_file(tmp_path, [record("one"), "{broken", record("two")])
    ).run()

    assert (summary.read, summary.valid, summary.created, summary.failed) == (3, 2, 2, 1)
    assert [item.project_name for item in client.created] == ["one", "two"]
    assert not summary.successful


def test_backend_failure_does_not_stop_later_records(tmp_path: Path) -> None:
    client = StubClient({"failure"})

    summary = CodingSessionIngestionJob(
        client,
        input_file(tmp_path, [record("one"), record("failure"), record("two")]),
    ).run()

    assert (summary.read, summary.valid, summary.created, summary.failed) == (3, 3, 2, 1)
    assert [item.project_name for item in client.created] == ["one", "two"]
    assert not summary.successful
