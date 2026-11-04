import asyncio
import os
import unittest
from unittest.mock import patch
from safedrive.models.coach import Coach, FALLBACK


class CoachTest(unittest.TestCase):
    def test_no_model_is_explicit_offline_fallback(self):
        with patch.dict(os.environ, {"OLLAMA_MODEL": ""}):
            self.assertEqual(asyncio.run(Coach(None).question()), (FALLBACK, "offline"))


if __name__ == "__main__":
    unittest.main()
