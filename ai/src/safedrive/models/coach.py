"""Select an approved English rest-stop question with the OpenAI Responses API."""

import os

import httpx


FALLBACK = "Where is the next safe place to pull over?"
QUESTIONS = (
    FALLBACK,
    "Can you find a safe place to park and rest?",
    "Is there a safe rest area nearby?",
)
INSTRUCTIONS = (
    "Select one English voice prompt for a drowsiness alert. "
    "The app has already told the driver to pull over and rest. "
    "Return exactly one of the supplied questions with no other text."
)


class Coach:
    def __init__(self, client: httpx.AsyncClient):
        self.client = client

    async def question(self) -> tuple[str, str]:
        key = os.getenv("OPENAI_API_KEY", "").strip()
        model = os.getenv("OPENAI_MODEL", "").strip()
        if not key or not model:
            return FALLBACK, "offline"
        try:
            response = await self.client.post(
                "https://api.openai.com/v1/responses",
                headers={"Authorization": f"Bearer {key}"},
                json={
                    "model": model,
                    "instructions": INSTRUCTIONS,
                    "input": "\n".join(QUESTIONS),
                    "max_output_tokens": 128,
                    "store": False,
                },
            )
            response.raise_for_status()
            body = response.json()
            if body.get("status") == "completed":
                messages = [
                    part["text"]
                    for item in body["output"] if item["type"] == "message"
                    for part in item["content"] if part["type"] == "output_text"
                ]
                question = " ".join(messages).strip()
                if question in QUESTIONS:
                    return question, "openai"
        except (httpx.HTTPError, ValueError, KeyError, TypeError, AttributeError):
            pass
        return FALLBACK, "offline"
