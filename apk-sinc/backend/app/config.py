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

    # Backend de persistencia: "supabase" (producao) ou "sqlite" (dev local
    # sem depender de um projeto Supabase configurado; testes automatizados
    # sempre usam SqliteRepository diretamente, sem passar por este flag).
    db_backend: str = "supabase"

    # Supabase (PostgreSQL) - banco principal em producao.
    # A SERVICE_ROLE_KEY tem privilegios administrativos e NUNCA deve ir
    # para o Android nem para qualquer cliente publico - somente o backend
    # a utiliza.
    supabase_url: str = ""
    supabase_service_role_key: str = ""

    # SQLite - usado apenas quando db_backend=sqlite (desenvolvimento local
    # sem Supabase configurado) ou em testes.
    sqlite_database_url: str = "sqlite:///./sinc.db"

    # Valores de fallback para monitor_settings caso a tabela ainda nao
    # tenha sido semeada (ver migrations/004_seed_settings.sql). A fonte da
    # verdade em producao e sempre a tabela `monitor_settings` no Supabase.
    default_warning_threshold_minutes: int = 5
    default_critical_threshold_minutes: int = 15
    default_check_interval_seconds: int = 60

    # Chave exigida no header X-API-Key. Vazia = autenticacao desligada
    # (apenas para desenvolvimento local); em producao DEVE ser definida.
    # Ver app/security.py.
    api_key: str = ""

    firebase_credentials_path: str = "./firebase-adminsdk.json"
    fcm_topic: str = "sinc-alerts"
    fcm_dry_run: bool = True

    api_host: str = "0.0.0.0"
    api_port: int = 8000
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()
