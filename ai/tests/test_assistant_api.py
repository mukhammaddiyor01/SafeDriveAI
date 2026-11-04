import os
import unittest
from unittest.mock import patch
import httpx
from fastapi.testclient import TestClient
from safedrive.controllers.assistant import app
from safedrive.models.coach import Coach, FALLBACK


class AssistantApiTest(unittest.TestCase):
    def test_health_and_offline_question(self):
        with patch.dict(os.environ, {"OLLAMA_MODEL": ""}), TestClient(app) as client:
            self.assertEqual(client.get("/health").json(), {"status": "ok", "camera_uploads": False})
            self.assertEqual(client.post("/question").json(), {"text": FALLBACK, "source": "offline"})

    def test_ollama_receives_only_text(self):
        import asyncio
        import json

        def respond(request):
            payload = json.loads(request.content)
            self.assertFalse(payload["stream"])
            self.assertNotIn("images", str(payload))
            return httpx.Response(200, json={"message": {"content": "Keyingi dam olish joyi qayerda?"}})

        async def run():
            async with httpx.AsyncClient(transport=httpx.MockTransport(respond)) as client:
                return await Coach(client).question()

        with patch.dict(os.environ, {"OLLAMA_MODEL": "test-model"}):
            self.assertEqual(asyncio.run(run())[1], "ollama")

    def test_unavailable_model_falls_back(self):
        import asyncio

        async def run():
            async with httpx.AsyncClient(transport=httpx.MockTransport(
                lambda request: httpx.Response(404, json={"error": "model missing"})
            )) as client:
                return await Coach(client).question()

        with patch.dict(os.environ, {"OLLAMA_MODEL": "missing"}):
            self.assertEqual(asyncio.run(run()), (FALLBACK, "offline"))

    def test_malformed_model_reply_falls_back(self):
        import asyncio

        async def run():
            async with httpx.AsyncClient(transport=httpx.MockTransport(
                lambda request: httpx.Response(200, json={"message": {"content": 42}})
            )) as client:
                return await Coach(client).question()

        with patch.dict(os.environ, {"OLLAMA_MODEL": "test-model"}):
            self.assertEqual(asyncio.run(run()), (FALLBACK, "offline"))
