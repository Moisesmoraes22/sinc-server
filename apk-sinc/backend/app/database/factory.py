from app.config import get_settings
from app.database.base import Repository


def build_repository() -> Repository:
    settings = get_settings()
    if settings.db_backend == "sqlite":
        from app.database.sqlite_repository import SqliteRepository

        return SqliteRepository(settings.sqlite_database_url)

    from app.database.supabase_repository import SupabaseRepository

    return SupabaseRepository()
