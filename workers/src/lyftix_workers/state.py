from __future__ import annotations

import json
import os
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path


class CheckpointError(RuntimeError):
    """Raised when worker state cannot be safely loaded or stored."""


@dataclass(frozen=True)
class GitHubCheckpoint:
    event_id: str
    occurred_at: str


class CheckpointStore:
    def __init__(self, path: Path) -> None:
        self.path = path.expanduser()

    def validate_path(self) -> None:
        try:
            self.path.parent.mkdir(parents=True, exist_ok=True)
        except OSError as exc:
            raise CheckpointError(
                f"Cannot create worker state directory: {self.path.parent}"
            ) from exc
        if self.path.exists() and not self.path.is_file():
            raise CheckpointError(f"Worker state path is not a file: {self.path}")

    def load(self) -> GitHubCheckpoint | None:
        if not self.path.exists():
            return None
        try:
            payload = json.loads(self.path.read_text(encoding="utf-8"))
            event_id = payload["event_id"]
            occurred_at = payload["occurred_at"]
        except (OSError, ValueError, KeyError, TypeError) as exc:
            raise CheckpointError(f"Worker state is corrupted or unreadable: {self.path}") from exc
        if not isinstance(event_id, str) or not event_id:
            raise CheckpointError(f"Worker state has an invalid event_id: {self.path}")
        if not isinstance(occurred_at, str) or not occurred_at:
            raise CheckpointError(f"Worker state has an invalid occurred_at: {self.path}")
        return GitHubCheckpoint(event_id=event_id, occurred_at=occurred_at)

    def save(self, checkpoint: GitHubCheckpoint) -> None:
        self.validate_path()
        temporary_path: Path | None = None
        try:
            with tempfile.NamedTemporaryFile(
                mode="w",
                encoding="utf-8",
                dir=self.path.parent,
                prefix=f".{self.path.name}.",
                suffix=".tmp",
                delete=False,
            ) as temporary:
                json.dump(asdict(checkpoint), temporary, separators=(",", ":"))
                temporary.write("\n")
                temporary.flush()
                os.fsync(temporary.fileno())
                temporary_path = Path(temporary.name)
            os.replace(temporary_path, self.path)
        except OSError as exc:
            if temporary_path is not None:
                temporary_path.unlink(missing_ok=True)
            raise CheckpointError(f"Cannot atomically write worker state: {self.path}") from exc
