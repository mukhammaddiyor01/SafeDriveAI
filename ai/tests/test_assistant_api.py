import asyncio
import json
import unittest
from unittest.mock import patch

import httpx
from fastapi.testclient import TestClient

from safedrive.controllers.assistant import app
from safedrive.models.coach import Coach, FALLBACK, QUESTIONS


class AssistantApiTest(unittest.TestCase):
    def test_health_and_offline_question(self):
        with patch.dict("os.environ", {"OPENAI_API_KEY": ""}), TestClient(app) as client:
            self.assertEqual(client.get("/health").json(), {"status": "ok", "camera_uploads": False})
            self.assertEqual(client.post("/question").json(), {"text": FALLBACK, "source": "offline"})

    def question_with(self, respond):
        async def run():
            async with httpx.AsyncClient(transport=httpx.MockTransport(respond)) as client:
                return await Coach(client).question()

        with patch.dict("os.environ", {"OPENAI_API_KEY": "test-key", "OPENAI_MODEL": "test-model"}):
            return asyncio.run(run())

    @staticmethod
    def completed(text):
        return httpx.Response(200, json={
            "status": "completed",
            "output": [
                {"type": "reasoning"},
                {"type": "message", "content": [{"type": "output_text", "text": text}]},
            ],
        })

    def test_openai_receives_only_the_predefined_english_prompt(self):
        def respond(request):
            self.assertEqual(str(request.url), "https://api.openai.com/v1/responses")
            self.assertEqual(request.headers["Authorization"], "Bearer test-key")
            payload = json.loads(request.content)
            self.assertFalse(payload["store"])
            self.assertEqual(payload["input"].splitlines(), list(QUESTIONS))
            return self.completed(QUESTIONS[1])

        self.assertEqual(self.question_with(respond), (QUESTIONS[1], "openai"))

    def test_unavailable_model_falls_back(self):
        self.assertEqual(self.question_with(lambda request: httpx.Response(503)), (FALLBACK, "offline"))

    def test_unapproved_or_malformed_reply_falls_back(self):
        for text in ("Keep driving.", 42):
            with self.subTest(text=text):
                self.assertEqual(self.question_with(lambda request: self.completed(text)), (FALLBACK, "offline"))
