"""Token-only app for deployment behind an HTTPS endpoint."""

from contextlib import asynccontextmanager
from datetime import datetime, timedelta, timezone
from secrets import compare_digest

import httpx
from fastapi import FastAPI, Header, HTTPException, Response

from safedrive.models.gemini_conversation import gemini_setting


async def issue_live_token(client: httpx.AsyncClient, key: str):
    if not key:
        raise HTTPException(503, "Gemini Live requires GEMINI_API")
    expires = datetime.now(timezone.utc) + timedelta(minutes=2)
    try:
        response = await client.post(
            "https://generativelanguage.googleapis.com/v1beta/auth_tokens",
            headers={"x-goog-api-key": key},
            json={"uses": 1, "expireTime": expires.isoformat()},
        )
        response.raise_for_status()
        return {"token": response.json()["name"], "model": "models/gemini-3.8-live"}
    except (httpx.HTTPError, ValueError, KeyError):
        raise HTTPException(503, "Gemini Live could not issue a token") from None


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with httpx.AsyncClient(timeout=8.0) as client:
        app.state.http = client
        app.state.key = gemini_setting("GEMINI_API")
        app.state.broker_token = gemini_setting("SAFEDRIVE_LIVE_BROKER_TOKEN")
        yield


app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)


@app.post("/live/token")
async def live_token(response: Response, authorization: str = Header(default="")):
    secret = app.state.broker_token
    if len(secret) < 32:
        raise HTTPException(503, "Live broker authentication is not configured")
    if not compare_digest(authorization.encode(), f"Bearer {secret}".encode()):
        raise HTTPException(401, "Live broker access denied")
    response.headers["Cache-Control"] = "no-store"
    return await issue_live_token(app.state.http, app.state.key)
