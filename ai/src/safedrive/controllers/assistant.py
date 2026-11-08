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
from safedrive.models.gemini_conversation import GeminiConversation, gemini_setting


logger = logging.getLogger("uvicorn.error")


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with httpx.AsyncClient(timeout=8.0) as client:
        app.state.coach = Coach(client)
        path = Path(os.getenv("SAFEDRIVE_CONVERSATION_FILE", "~/.safedriveai/conversation-id")).expanduser()
        key = gemini_setting("GEMINI_API")
        app.state.provider = "gemini" if key else "openai"
        app.state.conversation = (
            GeminiConversation(client, key, gemini_setting("GEMINI_MODEL") or "gemini-3.1-flash-lite")
            if key else DriverConversation(client, path)
        )
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
    provider = "Gemini" if app.state.provider == "gemini" else "OpenAI"
    try:
        conversation_id = await app.state.conversation.connect()
        return {"conversation_id": conversation_id}
    except httpx.HTTPStatusError as error:
        status = error.response.status_code
        if status in (401, 403):
            detail = f"{provider} rejected access. Check the API key and project permissions."
        elif status == 404:
            detail = f"The configured model or conversation is unavailable to this {provider} project."
        elif status == 429:
            detail = f"{provider} usage limit reached. Check project quota or billing."
        else:
            detail = f"{provider} returned HTTP {status}."
    except httpx.RequestError:
        detail = f"The assistant could not reach {provider}. Check the Mac internet connection."
    except (OSError, ValueError, KeyError, TypeError):
        missing = [name for name in ("OPENAI_API_KEY", "OPENAI_MODEL") if not os.getenv(name, "").strip()]
        if app.state.provider == "gemini":
            missing = []
        detail = ("Missing server configuration: " + ", ".join(missing)) if missing else (
            "The assistant could not load or save the conversation."
        )
    logger.warning("SafeDriveAI connection failed: %s", detail)
    raise HTTPException(503, detail)


@app.post("/turn")
async def turn(turn: DriverTurn):
    try:
        text = await app.state.conversation.turn(turn.reply)
        return {"text": text, "source": app.state.provider}
    except (httpx.HTTPError, OSError, ValueError, KeyError, TypeError):
        raise HTTPException(503, "Voice coach response unavailable") from None
