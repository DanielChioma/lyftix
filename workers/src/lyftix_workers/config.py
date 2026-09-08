import os
from pathlib import Path

from pydantic import AnyHttpUrl, Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class WorkerSettings(BaseSettings):
    """Environment-only configuration for one-shot worker runs."""

    model_config = SettingsConfigDict(extra="ignore")

    github_token: SecretStr = Field(validation_alias="GITHUB_TOKEN")
    github_username: str = Field(min_length=1, validation_alias="GITHUB_USERNAME")
    lyftix_api_base_url: AnyHttpUrl = Field(validation_alias="LYFTIX_API_BASE_URL")
    github_api_base_url: AnyHttpUrl = Field(
        default=AnyHttpUrl("https://api.github.com"),
        validation_alias="GITHUB_API_BASE_URL",
    )
    http_timeout_seconds: float = Field(
        default=10.0,
        gt=0,
        validation_alias="HTTP_TIMEOUT_SECONDS",
    )
    github_events_per_page: int = Field(
        default=30,
        ge=1,
        le=100,
        validation_alias="GITHUB_EVENTS_PER_PAGE",
    )
    github_max_pages_per_run: int = Field(
        default=3,
        ge=1,
        validation_alias="GITHUB_MAX_PAGES_PER_RUN",
    )
    github_ingestion_interval_seconds: int = Field(
        default=900,
        ge=60,
        validation_alias="GITHUB_INGESTION_INTERVAL_SECONDS",
    )
    worker_state_path: Path = Field(
        default=Path("~/.local/state/lyftix/github.json"),
        validation_alias="WORKER_STATE_PATH",
    )


class CodingSessionSettings(BaseSettings):
    """Environment-only configuration for coding-session ingestion."""

    model_config = SettingsConfigDict(extra="ignore")

    lyftix_api_base_url: AnyHttpUrl = Field(validation_alias="LYFTIX_API_BASE_URL")
    coding_sessions_input_path: Path = Field(validation_alias="CODING_SESSIONS_INPUT_PATH")
    coding_session_default_source: str | None = Field(
        default=None,
        max_length=100,
        validation_alias="CODING_SESSION_DEFAULT_SOURCE",
    )
    http_timeout_seconds: float = Field(
        default=10.0,
        gt=0,
        validation_alias="HTTP_TIMEOUT_SECONDS",
    )

    @field_validator("coding_sessions_input_path")
    @classmethod
    def validate_input_path(cls, value: Path) -> Path:
        path = value.expanduser()
        if not path.is_file():
            raise ValueError("coding sessions input path must be an existing file")
        if not os.access(path, os.R_OK):
            raise ValueError("coding sessions input path must be readable")
        return path

    @field_validator("coding_session_default_source")
    @classmethod
    def validate_default_source(cls, value: str | None) -> str | None:
        if value is not None and not value.strip():
            raise ValueError("coding session default source must not be blank")
        return value
