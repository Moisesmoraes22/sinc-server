"""Estado compartilhado da aplicacao FastAPI (repository) e dependencias
usadas pelos routers. Mantido simples de proposito: um unico repository e
criado no startup (app/main.py) e reutilizado por toda a aplicacao -
Supabase e SQLite sao ambos seguros para uso concorrente aqui (supabase-py e
sincrono/stateless por chamada; SqliteRepository abre uma sessao nova por
operacao).
"""

from app.database.base import Repository

app_state: dict = {
    "repository": None,
}


def get_repository() -> Repository:
    repository = app_state.get("repository")
    if repository is None:
        raise RuntimeError("Repository ainda nao foi inicializado (lifespan nao rodou)")
    return repository
