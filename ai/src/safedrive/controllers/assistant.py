"""Run locally: uvicorn safedrive.controllers.assistant:app --host 127.0.0.1 --port 8765"""

from contextlib import asynccontextmanager
import httpx
from fastapi import FastAPI
from safedrive.models.coach import Coach


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with httpx.AsyncClient(timeout=8.0) as client:
        app.state.coach = Coach(client)
        yield


app = FastAPI(title="SafeDriveAI local voice assistant", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "ok", "camera_uploads": False}


@app.post("/question")
async def question():
    text, source = await app.state.coach.question()
    return {"text": text, "source": source}
