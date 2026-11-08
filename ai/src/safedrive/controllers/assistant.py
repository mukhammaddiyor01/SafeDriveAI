"""Run locally: uvicorn safedrive.controllers.assistant:app --host 127.0.0.1 --port 8765"""

from contextlib import asynccontextmanager
import logging
import os
from pathlib import Path

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from safedrive.models.coach import Coach
from safedrive.models.conversation import DriverConversation


logger = logging.getLogger("uvicorn.error")


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
    except httpx.HTTPStatusError as error:
        status = error.response.status_code
        if status in (401, 403):
            detail = "OpenAI rejected access. Check the API key and project permissions."
        elif status == 404:
            detail = "The saved conversation is unavailable to this OpenAI project."
        elif status == 429:
            detail = "OpenAI usage limit reached. Check project quota or billing."
        else:
            detail = f"OpenAI returned HTTP {status}."
    except httpx.RequestError:
        detail = "The assistant could not reach OpenAI. Check the Mac internet connection."
    except (OSError, ValueError, KeyError, TypeError):
        missing = [name for name in ("OPENAI_API_KEY", "OPENAI_MODEL") if not os.getenv(name, "").strip()]
        detail = ("Missing server configuration: " + ", ".join(missing)) if missing else (
            "The assistant could not load or save the conversation."
        )
    logger.warning("SafeDriveAI connection failed: %s", detail)
    raise HTTPException(503, detail)


@app.post("/turn")
async def turn(turn: DriverTurn):
    try:
        text = await app.state.conversation.turn(turn.reply)
        return {"text": text, "source": "openai"}
    except (httpx.HTTPError, OSError, ValueError, KeyError, TypeError):
        raise HTTPException(503, "ChatGPT response unavailable") from None
