from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database.db import get_session
from app.database.repository import ServerRepository
from app.models.schemas import ServerListResponse, ServerOut

router = APIRouter(prefix="/api/servers", tags=["servers"])


def _session() -> Session:
    session = get_session()
    try:
        yield session
    finally:
        session.close()


@router.get("", response_model=ServerListResponse)
def list_servers(session: Session = Depends(_session)) -> ServerListResponse:
    servers = ServerRepository(session).list_all()
    return ServerListResponse(servers=[ServerOut.model_validate(s) for s in servers])


@router.get("/{server_id}", response_model=ServerOut)
def get_server(server_id: str, session: Session = Depends(_session)) -> ServerOut:
    server = ServerRepository(session).get(server_id)
    if server is None:
        raise HTTPException(status_code=404, detail="Servidor nao encontrado")
    return ServerOut.model_validate(server)
