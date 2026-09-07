from pydantic import AnyHttpUrl, Field, SecretStr
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
