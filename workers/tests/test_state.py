import json
from pathlib import Path

import pytest

from lyftix_workers.state import CheckpointError, CheckpointStore, GitHubCheckpoint


def test_missing_state_is_valid_first_run(tmp_path: Path) -> None:
    assert CheckpointStore(tmp_path / "state.json").load() is None


def test_loads_existing_checkpoint(tmp_path: Path) -> None:
    path = tmp_path / "state.json"
    path.write_text('{"event_id":"123","occurred_at":"2026-09-07T12:00:00Z"}\n')

    checkpoint = CheckpointStore(path).load()

    assert checkpoint == GitHubCheckpoint("123", "2026-09-07T12:00:00Z")


def test_saves_checkpoint_atomically(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    path = tmp_path / "state" / "github.json"
    replacements: list[tuple[Path, Path]] = []
    original_replace = __import__("os").replace

    def recording_replace(source: Path, destination: Path) -> None:
        replacements.append((Path(source), Path(destination)))
        assert Path(source).parent == path.parent
        original_replace(source, destination)

    monkeypatch.setattr("lyftix_workers.state.os.replace", recording_replace)
    CheckpointStore(path).save(GitHubCheckpoint("456", "2026-09-08T12:00:00Z"))

    assert replacements and replacements[0][1] == path
    assert json.loads(path.read_text()) == {
        "event_id": "456",
        "occurred_at": "2026-09-08T12:00:00Z",
    }
    assert list(path.parent.glob("*.tmp")) == []


def test_corrupted_state_fails_deliberately(tmp_path: Path) -> None:
    path = tmp_path / "state.json"
    path.write_text("not-json")

    with pytest.raises(CheckpointError, match="corrupted or unreadable"):
        CheckpointStore(path).load()
