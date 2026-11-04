"""Local Ollama adapter. Raw camera images are never sent to the LLM."""

import os


FALLBACK = "Xavfsiz joyda to‘xtab, dam oling. Keyingi dam olish joyi qayerda?"
SYSTEM = (
    "You are SafeDriveAI, a short Uzbek voice assistant. The driver may be drowsy. "
    "Reply in Uzbek in at most two short sentences. Tell the driver to stop safely "
    "and rest, then ask one easy question about their next rest stop. "
    "Never suggest games, mental arithmetic, looking at a screen, caffeine as a cure, "
    "or continuing to drive. Never claim to prevent sleep. Do not provide medical advice. "
    "Return only the spoken words."
)


class Coach:
    def __init__(self, client):
        self.client = client

    async def question(self) -> tuple[str, str]:
        model = os.getenv("OLLAMA_MODEL", "")
        if not model:
            return FALLBACK, "offline"
        import httpx
        try:
            response = await self.client.post(
                os.getenv("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/") + "/api/chat",
                json={"model": model, "stream": False,
                      "messages": [{"role": "system", "content": SYSTEM},
                                   {"role": "user", "content": "Ask about the next safe rest stop."}],
                      "options": {"temperature": 0.3, "num_predict": 100}},
            )
            response.raise_for_status()
            content = response.json()["message"]["content"].strip()
            if not content or len(content) > 300:
                return FALLBACK, "offline"
            # A fixed safety sentence is spoken by Android before any generated text.
            return content, "ollama"
        except (httpx.HTTPError, ValueError, KeyError, TypeError, AttributeError):
            return FALLBACK, "offline"
