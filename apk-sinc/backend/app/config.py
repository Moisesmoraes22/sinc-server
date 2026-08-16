from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    source_mode: str = "mock"  # "real" | "mock"
    source_url: str = "http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/"
    source_timeout_seconds: float = 10.0

    poll_interval_seconds: int = 30
    retry_attempts: int = 3
    retry_backoff_seconds: float = 2.0

    failure_threshold_attention: int = 1
    failure_threshold_offline: int = 3

    database_url: str = "sqlite:///./sinc.db"

    firebase_credentials_path: str = "./firebase-adminsdk.json"
    fcm_topic: str = "sinc-alerts"
    fcm_dry_run: bool = True

    api_host: str = "0.0.0.0"
    api_port: int = 8000
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()
