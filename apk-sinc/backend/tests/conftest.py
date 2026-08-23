import os

# Garante que qualquer inicializacao real da aplicacao (lifespan do FastAPI
# em testes de API) use SQLite em memoria em vez de tentar falar com um
# Supabase real - nenhum teste depende de credenciais/rede do Supabase.
os.environ.setdefault("DB_BACKEND", "sqlite")
os.environ.setdefault("SQLITE_DATABASE_URL", "sqlite:///:memory:")
os.environ.setdefault("FCM_DRY_RUN", "true")

import pytest

from app.database.sqlite_repository import SqliteRepository


@pytest.fixture()
def repository() -> SqliteRepository:
    """Repository SQLite isolado (banco em memoria) para cada teste - nenhum
    teste depende do Supabase real, conforme exigido pela tarefa."""
    return SqliteRepository("sqlite:///:memory:")
