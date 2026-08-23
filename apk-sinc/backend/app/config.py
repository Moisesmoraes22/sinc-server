from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

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

    firebase_credentials_path: str = "./firebase-adminsdk.json"
    fcm_topic: str = "omg-sinc-lembretes"
    fcm_dry_run: bool = True

    api_host: str = "0.0.0.0"
    api_port: int = 8000
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()
