import pytest

from app.database.db import init_db


@pytest.fixture()
def db_session():
    init_db("sqlite:///:memory:")
    from app.database.db import get_session

    session = get_session()
    yield session
    session.close()
