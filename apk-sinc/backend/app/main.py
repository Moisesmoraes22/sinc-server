import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import get_settings
from app.database.factory import build_repository
from app.dependencies import app_state
from app.routers import devices, habits, health, home, metrics, profile


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logging.basicConfig(level=settings.log_level)

    repository = build_repository()
    app_state["repository"] = repository

    yield

    app_state["repository"] = None


def create_app() -> FastAPI:
    app = FastAPI(title="OMG SINC Backend", version="2.0.0", lifespan=lifespan)

    app.include_router(profile.router)
    app.include_router(habits.router)
    app.include_router(metrics.router)
    app.include_router(home.router)
    app.include_router(devices.router)
    app.include_router(health.router)

    return app


app = create_app()
