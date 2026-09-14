import pytest
from pydantic import ValidationError

from lyftix_workers.config import WorkerSettings


def test_loads_required_environment_configuration(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("GITHUB_TOKEN", "secret")
    monkeypatch.setenv("GITHUB_USERNAME", "octocat")
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")
    monkeypatch.setenv("LYFTIX_WORKER_USERNAME", "worker")
    monkeypatch.setenv("LYFTIX_WORKER_PASSWORD", "worker-password")

    settings = WorkerSettings()

    assert settings.github_token.get_secret_value() == "secret"
    assert settings.github_username == "octocat"
    assert str(settings.github_api_base_url) == "https://api.github.com/"


def test_missing_required_configuration_fails_clearly(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in (
        "GITHUB_TOKEN",
        "GITHUB_USERNAME",
        "LYFTIX_API_BASE_URL",
        "LYFTIX_WORKER_USERNAME",
        "LYFTIX_WORKER_PASSWORD",
    ):
        monkeypatch.delenv(name, raising=False)

    with pytest.raises(ValidationError) as error:
        WorkerSettings()

    message = str(error.value)
    assert "GITHUB_TOKEN" in message
    assert "GITHUB_USERNAME" in message
    assert "LYFTIX_API_BASE_URL" in message
    assert "LYFTIX_WORKER_USERNAME" in message
    assert "LYFTIX_WORKER_PASSWORD" in message


@pytest.mark.parametrize(
    ("name", "value"),
    [("GITHUB_MAX_PAGES_PER_RUN", "0"), ("GITHUB_INGESTION_INTERVAL_SECONDS", "59")],
)
def test_rejects_unsafe_scheduling_configuration(
    monkeypatch: pytest.MonkeyPatch,
    name: str,
    value: str,
) -> None:
    monkeypatch.setenv("GITHUB_TOKEN", "secret")
    monkeypatch.setenv("GITHUB_USERNAME", "octocat")
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")
    monkeypatch.setenv("LYFTIX_WORKER_USERNAME", "worker")
    monkeypatch.setenv("LYFTIX_WORKER_PASSWORD", "worker-password")
    monkeypatch.setenv(name, value)

    with pytest.raises(ValidationError):
        WorkerSettings()
