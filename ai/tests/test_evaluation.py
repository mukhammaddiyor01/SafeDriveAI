import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest

import cv2
import numpy as np
import pandas as pd
import torch

from safedrive.controllers.evaluate import evaluate
from safedrive.models.dataset import read_prepared_manifest
from safedrive.models.eye_model import EyeClassifier
from safedrive.models.provenance import file_sha256, manifest_fingerprint


class EvaluationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        rows = []
        for split in ("train", "val", "test"):
            for label in ("open", "closed"):
                path = self.root / f"{split}_{label}.png"
                cv2.imwrite(str(path), np.full((8, 8), 120, dtype=np.uint8))
                rows.append({"image_path": str(path), "subject_id": split, "label": label,
                             "view": "unknown", "split": split, "glasses": "1"})
        self.manifest = self.root / "manifest.csv"
        pd.DataFrame(rows).to_csv(self.manifest, index=False)
        model = EyeClassifier()
        for parameter in model.parameters():
            torch.nn.init.zeros_(parameter)
        self.weights = self.root / "eye_state.pt"
        torch.save(model.state_dict(), self.weights)
        self.card = {"status": "training complete", "best_epoch": 1,
                     "checkpoint_sha256": file_sha256(self.weights),
                     "dataset_fingerprint": manifest_fingerprint(read_prepared_manifest(self.manifest))}
        (self.root / "model-card.json").write_text(json.dumps(self.card))

    def test_closed_probability_and_confusion_counts_are_not_inverted(self):
        with contextlib.redirect_stdout(io.StringIO()):
            summary = evaluate(self.manifest, self.weights, self.root / "report", batch_size=1)
        self.assertEqual(summary["metrics"]["tp"], 1)
        self.assertEqual(summary["metrics"]["fp"], 1)
        self.assertEqual(summary["metrics"]["fn"], 0)
        self.assertEqual(summary["metrics"]["balanced_accuracy"], 0.5)
        report = pd.read_csv(self.root / "report/metrics_by_condition.csv")
        self.assertIn("glasses", set(report.condition))

    def test_modified_checkpoint_is_rejected_before_loading(self):
        with self.weights.open("ab") as stream:
            stream.write(b"changed")
        with self.assertRaisesRegex(ValueError, "Checkpoint does not match"):
            evaluate(self.manifest, self.weights, self.root / "report")

    def test_different_manifest_is_rejected(self):
        frame = pd.read_csv(self.manifest)
        frame["view"] = "left"
        frame.to_csv(self.manifest, index=False)
        with self.assertRaisesRegex(ValueError, "manifest does not match"):
            evaluate(self.manifest, self.weights, self.root / "report")

    def test_unfinished_training_cannot_be_used_for_held_out_evaluation(self):
        self.card["status"] = "training in progress"
        (self.root / "model-card.json").write_text(json.dumps(self.card))
        with self.assertRaisesRegex(ValueError, "Finish"):
            evaluate(self.manifest, self.weights, self.root / "report")
