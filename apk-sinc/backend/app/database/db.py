from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from app.config import get_settings
from app.models.orm import Base


def make_engine(database_url: str | None = None):
    url = database_url or get_settings().database_url
    is_sqlite = url.startswith("sqlite")
    connect_args = {"check_same_thread": False} if is_sqlite else {}
    is_memory = url in ("sqlite:///:memory:", "sqlite://")
    poolclass = StaticPool if is_memory else None
    engine = create_engine(url, connect_args=connect_args, poolclass=poolclass)
    Base.metadata.create_all(engine)
    return engine


_engine = None
_SessionLocal: sessionmaker | None = None


def init_db(database_url: str | None = None) -> None:
    global _engine, _SessionLocal
    _engine = make_engine(database_url)
    _SessionLocal = sessionmaker(bind=_engine, autoflush=False, autocommit=False)


def get_session() -> Session:
    if _SessionLocal is None:
        init_db()
    assert _SessionLocal is not None
    return _SessionLocal()
