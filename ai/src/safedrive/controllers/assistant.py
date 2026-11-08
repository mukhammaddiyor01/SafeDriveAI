"""Run locally: uvicorn safedrive.controllers.assistant:app --host 127.0.0.1 --port 8765"""

from contextlib import asynccontextmanager
import os
from pathlib import Path

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from safedrive.models.coach import Coach
from safedrive.models.conversation import DriverConversation


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with httpx.AsyncClient(timeout=8.0) as client:
        app.state.coach = Coach(client)
        path = Path(os.getenv("SAFEDRIVE_CONVERSATION_FILE", "~/.safedriveai/conversation-id")).expanduser()
        app.state.conversation = DriverConversation(client, path)
        yield


app = FastAPI(title="SafeDriveAI local voice assistant", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "ok", "camera_uploads": False}


@app.post("/question")
async def question():
    text, source = await app.state.coach.question()
    return {"text": text, "source": source}


class DriverTurn(BaseModel):
    reply: str | None = Field(default=None, min_length=1, max_length=300)


@app.post("/connect")
async def connect():
    try:
        conversation_id = await app.state.conversation.connect()
        return {"conversation_id": conversation_id}
    except (httpx.HTTPError, OSError, ValueError, KeyError, TypeError):
        raise HTTPException(503, "ChatGPT connection unavailable") from None


@app.post("/turn")
async def turn(turn: DriverTurn):
    try:
        text = await app.state.conversation.turn(turn.reply)
        return {"text": text, "source": "openai"}
    except (httpx.HTTPError, OSError, ValueError, KeyError, TypeError):
        raise HTTPException(503, "ChatGPT response unavailable") from None
