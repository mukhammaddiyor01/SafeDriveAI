import asyncio
import json

import httpx

from safedrive.models.coach import QUESTIONS
from safedrive.models.conversation import DriverConversation, FOLLOW_UPS


def test_reconnect_and_later_alerts_reuse_the_same_conversation(tmp_path, monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setenv("OPENAI_MODEL", "test-model")
    path = tmp_path / "conversation-id"
    calls = []
    replies = iter([QUESTIONS[0], FOLLOW_UPS[0], QUESTIONS[1]])

    def respond(request):
        calls.append((request.method, request.url.path))
        if request.url.path == "/v1/conversations":
            return httpx.Response(200, json={"id": "conv_test"})
        if request.url.path == "/v1/conversations/conv_test":
            return httpx.Response(200, json={"id": "conv_test"})
        payload = json.loads(request.content)
        assert payload["conversation"] == "conv_test"
        if len(calls) == 4:
            assert payload["input"] == "I found a rest area."
        return httpx.Response(200, json={
            "status": "completed",
            "output": [{"type": "message", "content": [{"type": "output_text", "text": next(replies)}]}],
        })

    async def run():
        async with httpx.AsyncClient(transport=httpx.MockTransport(respond)) as client:
            first = DriverConversation(client, path)
            assert await first.connect() == "conv_test"
            restarted = DriverConversation(client, path)
            assert await restarted.connect() == "conv_test"
            assert await restarted.turn(None) == QUESTIONS[0]
            assert await restarted.turn("I found a rest area.") == FOLLOW_UPS[0]
            assert await restarted.turn(None) == QUESTIONS[1]

    asyncio.run(run())
    assert calls.count(("POST", "/v1/conversations")) == 1
