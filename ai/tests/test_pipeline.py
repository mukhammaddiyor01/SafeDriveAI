"""Synthetic data checks wiring only, never real driver-monitoring accuracy."""

import tempfile
import json
import unittest
from pathlib import Path
import cv2
import numpy as np
import pandas as pd
import torch
from safedrive.models.dataset import prepare_manifest, read_manifest
from safedrive.models.eye_model import EyeClassifier
from safedrive.controllers.train import train
from safedrive.controllers.evaluate import evaluate


class PipelineTest(unittest.TestCase):
    def test_train_and_evaluate_subject_independent_manifest(self):
        torch.set_num_threads(1)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            rows = []
            for subject in range(50):
                for label, level in (("closed", 30), ("open", 220)):
                    filename = f"{subject}_{label}.png"
                    cv2.imwrite(str(root / filename), np.full((64, 64), level, dtype=np.uint8))
                    rows.append({"image_path": filename, "subject_id": str(subject),
                                 "label": label, "view": "front"})
            pd.DataFrame(rows).to_csv(root / "source.csv", index=False)
            prepare_manifest(root / "source.csv", root / "manifest.csv")
            split = pd.read_csv(root / "manifest.csv")
            self.assertTrue((split.groupby("subject_id").split.nunique() == 1).all())
            train(root / "manifest.csv", root / "model", epochs=1, seed=42)
            evaluate(root / "manifest.csv", root / "model/eye_state.pt", root / "evaluation")
            metrics = pd.read_csv(root / "evaluation/metrics_by_view.csv")
            self.assertEqual(int(metrics.samples.sum()), int((split.split == "test").sum()))
            card = json.loads((root / "model/model-card.json").read_text())
            self.assertEqual(card["completed_epochs"], 1)
            self.assertEqual(card["best_epoch"], 1)
            self.assertEqual(card["device"], "cpu")
            self.assertEqual(len(card["checkpoint_sha256"]), 64)
            self.assertEqual(len(card["dataset_fingerprint"]), 64)
            with self.assertRaisesRegex(ValueError, "not empty"):
                train(root / "manifest.csv", root / "model", epochs=1, seed=42)
            state = torch.load(root / "model/eye_state.pt", weights_only=True)
            model = EyeClassifier()
            model.load_state_dict(state)
            self.assertEqual(tuple(model(torch.zeros(2, 1, 64, 64)).shape), (2,))

    def test_missing_image_and_invalid_label_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "input.csv"
            path.write_text("image_path,subject_id,label,view\nmissing.png,a,open,front\n")
            with self.assertRaisesRegex(ValueError, "does not exist"):
                read_manifest(path)
            path.write_text("image_path,subject_id,label,view\nmissing.png,a,sleeping,front\n")
            with self.assertRaisesRegex(ValueError, "label"):
                read_manifest(path)
