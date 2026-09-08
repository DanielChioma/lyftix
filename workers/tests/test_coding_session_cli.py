import json
import sys
from pathlib import Path

import httpx
import pytest
from pydantic import ValidationError

import lyftix_workers.config as config_module
import lyftix_workers.main as main_module
from lyftix_workers.config import CodingSessionSettings


def clear_coding_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in (
        "LYFTIX_API_BASE_URL",
        "CODING_SESSIONS_INPUT_PATH",
        "CODING_SESSION_DEFAULT_SOURCE",
    ):
        monkeypatch.delenv(name, raising=False)


def test_missing_input_configuration_fails(monkeypatch: pytest.MonkeyPatch) -> None:
    clear_coding_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")

    with pytest.raises(ValidationError, match="CODING_SESSIONS_INPUT_PATH"):
        CodingSessionSettings()


def test_missing_input_file_fails(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    clear_coding_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")
    monkeypatch.setenv("CODING_SESSIONS_INPUT_PATH", str(tmp_path / "missing.jsonl"))

    with pytest.raises(ValidationError, match="existing file"):
        CodingSessionSettings()


def test_unreadable_input_file_fails(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    path = tmp_path / "sessions.jsonl"
    path.write_text("{}", encoding="utf-8")
    clear_coding_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")
    monkeypatch.setenv("CODING_SESSIONS_INPUT_PATH", str(path))
    monkeypatch.setattr(config_module.os, "access", lambda candidate, mode: False)

    with pytest.raises(ValidationError, match="must be readable"):
        CodingSessionSettings()


def test_valid_configuration_loads(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> None:
    path = tmp_path / "sessions.jsonl"
    path.write_text("{}", encoding="utf-8")
    clear_coding_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")
    monkeypatch.setenv("CODING_SESSIONS_INPUT_PATH", str(path))
    monkeypatch.setenv("CODING_SESSION_DEFAULT_SOURCE", "file")

    settings = CodingSessionSettings()

    assert settings.coding_sessions_input_path == path
    assert settings.coding_session_default_source == "file"


def test_cli_routes_coding_sessions_command(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(sys, "argv", ["lyftix-worker", "coding-sessions"])
    monkeypatch.setattr(main_module, "run_coding_sessions", lambda: 7)

    assert main_module.main() == 7


@pytest.mark.parametrize(("status_code", "expected_exit"), [(201, 0), (500, 1)])
def test_command_wires_input_to_backend_and_returns_summary_exit_code(
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
    status_code: int,
    expected_exit: int,
) -> None:
    path = tmp_path / "sessions.jsonl"
    path.write_text(
        json.dumps(
            {
                "projectName": "lyftix",
                "language": "Python",
                "startedAt": "2026-09-08T08:00:00Z",
                "endedAt": "2026-09-08T09:00:00Z",
            }
        ),
        encoding="utf-8",
    )
    clear_coding_environment(monkeypatch)
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://lyftix.test")
    monkeypatch.setenv("CODING_SESSIONS_INPUT_PATH", str(path))
    monkeypatch.setenv("CODING_SESSION_DEFAULT_SOURCE", "file")
    posted: list[dict] = []

    def handler(request: httpx.Request) -> httpx.Response:
        posted.append(json.loads(request.content))
        return httpx.Response(status_code, request=request)

    client_type = httpx.Client
    monkeypatch.setattr(
        main_module.httpx,
        "Client",
        lambda **kwargs: client_type(transport=httpx.MockTransport(handler), **kwargs),
    )

    assert main_module.run_coding_sessions() == expected_exit
    assert posted == [
        {
            "projectName": "lyftix",
            "language": "Python",
            "startedAt": "2026-09-08T08:00:00Z",
            "endedAt": "2026-09-08T09:00:00Z",
            "source": "file",
            "notes": None,
        }
    ]
