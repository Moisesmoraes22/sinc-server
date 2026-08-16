from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.models.orm import Base


def make_engine(database_url: str) -> Engine:
    is_sqlite = database_url.startswith("sqlite")
    connect_args = {"check_same_thread": False} if is_sqlite else {}
    is_memory = database_url in ("sqlite:///:memory:", "sqlite://")
    poolclass = StaticPool if is_memory else None
    engine = create_engine(database_url, connect_args=connect_args, poolclass=poolclass)
    Base.metadata.create_all(engine)
    return engine


def make_sessionmaker(database_url: str) -> sessionmaker:
    engine = make_engine(database_url)
    return sessionmaker(bind=engine, autoflush=False, autocommit=False)
