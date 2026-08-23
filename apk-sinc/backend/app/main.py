import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import get_settings
from app.database.factory import build_repository
from app.dependencies import app_state
from app.monitor.factory import build_source_client
from app.monitor.poller import Poller
from app.notifications.fcm_client import FcmClient
from app.routers import devices, events, health, mock, servers, sync_units
from app.services.monitor_service import MonitorService


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logging.basicConfig(level=settings.log_level)

    repository = build_repository()
    app_state["repository"] = repository

    source_client = build_source_client()
    monitor_service = MonitorService(source_client, repository, FcmClient())
    poller = Poller(monitor_service, settings.retry_attempts, settings.retry_backoff_seconds)
    poller.start()
    app_state["poller"] = poller

    yield

    await poller.stop()
    if hasattr(source_client, "close"):
        await source_client.close()
    app_state["repository"] = None
    app_state["poller"] = None


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(title="APK SINC Backend", version="0.2.0", lifespan=lifespan)

    app.include_router(sync_units.router)
    app.include_router(servers.router)
    app.include_router(events.router)
    app.include_router(health.router)
    app.include_router(devices.router)
    if settings.source_mode == "mock":
        app.include_router(mock.router)

    return app


app = create_app()
