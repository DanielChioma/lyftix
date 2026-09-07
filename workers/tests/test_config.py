import pytest
from pydantic import ValidationError

from lyftix_workers.config import WorkerSettings


def test_loads_required_environment_configuration(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("GITHUB_TOKEN", "secret")
    monkeypatch.setenv("GITHUB_USERNAME", "octocat")
    monkeypatch.setenv("LYFTIX_API_BASE_URL", "http://localhost:8080")

    settings = WorkerSettings()

    assert settings.github_token.get_secret_value() == "secret"
    assert settings.github_username == "octocat"
    assert str(settings.github_api_base_url) == "https://api.github.com/"


def test_missing_required_configuration_fails_clearly(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in ("GITHUB_TOKEN", "GITHUB_USERNAME", "LYFTIX_API_BASE_URL"):
        monkeypatch.delenv(name, raising=False)

    with pytest.raises(ValidationError) as error:
        WorkerSettings()

    message = str(error.value)
    assert "GITHUB_TOKEN" in message
    assert "GITHUB_USERNAME" in message
    assert "LYFTIX_API_BASE_URL" in message
