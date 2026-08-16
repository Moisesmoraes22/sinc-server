from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database.db import get_session
from app.database.repository import DeviceTokenRepository
from app.models.schemas import DeviceRegisterRequest

router = APIRouter(prefix="/api/devices", tags=["devices"])


def _session() -> Session:
    session = get_session()
    try:
        yield session
    finally:
        session.close()


@router.post("")
def register_device(payload: DeviceRegisterRequest, session: Session = Depends(_session)) -> dict:
    DeviceTokenRepository(session).register(payload.token, payload.platform)
    return {"status": "ok"}
