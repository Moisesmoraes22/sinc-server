from fastapi import APIRouter, Depends

from app.database.base import Repository
from app.dependencies import get_repository
from app.models.schemas import DeviceListResponse, DeviceOut, DeviceRegisterRequest

router = APIRouter(prefix="/api/devices", tags=["devices"])


@router.post("")
def register_device(payload: DeviceRegisterRequest, repository: Repository = Depends(get_repository)) -> dict:
    # `token`/`platform` sao os nomes de campo ja usados pelo app Android
    # (data/remote/ApiModels.kt: DeviceRegisterRequestDto) - registrados para
    # permitir lembretes de habito via push no futuro.
    repository.register_device(fcm_token=payload.token, device_name=payload.platform)
    return {"status": "ok"}


@router.get("", response_model=DeviceListResponse)
def list_devices(repository: Repository = Depends(get_repository)) -> DeviceListResponse:
    devices = repository.list_devices()
    return DeviceListResponse(devices=[DeviceOut.model_validate(d) for d in devices])
