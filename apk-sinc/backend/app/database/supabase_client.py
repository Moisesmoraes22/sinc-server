"""Wrapper fino sobre o cliente oficial `supabase-py`.

Usa sempre a SERVICE_ROLE_KEY (privilegios administrativos) - por isso este
modulo so deve ser importado pelo backend, nunca por codigo que rode no
dispositivo do usuario. A chave e lida exclusivamente de variavel de
ambiente (`SUPABASE_SERVICE_ROLE_KEY`, ver .env.example) e nunca deve ser
commitada nem embutida no APK.
"""

from functools import lru_cache

from supabase import Client, create_client

from app.config import get_settings


class SupabaseNotConfiguredError(RuntimeError):
    pass


@lru_cache
def get_supabase_client() -> Client:
    settings = get_settings()
    if not settings.supabase_url or not settings.supabase_service_role_key:
        raise SupabaseNotConfiguredError(
            "SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY precisam estar definidos no .env "
            "para usar db_backend=supabase. Veja docs/setup-supabase.md."
        )
    return create_client(settings.supabase_url, settings.supabase_service_role_key)
