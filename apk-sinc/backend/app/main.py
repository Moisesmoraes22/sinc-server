import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import get_settings
from app.database.db import init_db
from app.monitor.factory import build_source_client
from app.monitor.poller import Poller
from app.notifications.fcm_client import FcmClient
from app.routers import devices, events, health, mock, servers
from app.services.monitor_service import MonitorService

app_state: dict = {"last_poll_at": None}


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logging.basicConfig(level=settings.log_level)
    init_db()

    source_client = build_source_client()
    monitor_service = MonitorService(source_client, FcmClient())
    poller = Poller(monitor_service)
    poller.start()
    app_state["poller"] = poller

    yield

    await poller.stop()


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(title="APK SINC Backend", version="0.1.0", lifespan=lifespan)

    app.include_router(servers.router)
    app.include_router(events.router)
    app.include_router(health.router)
    app.include_router(devices.router)
    if settings.source_mode == "mock":
        app.include_router(mock.router)

    return app


app = create_app()
