import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import cv2
import numpy as np
import pandas as pd

from safedrive.controllers.train import train
from safedrive.models.dataset import prepare_manifest, read_prepared_manifest, select_test_subjects
from safedrive.models.eye_model import load_eye
from safedrive.models.provenance import file_sha256


class TrainTestUsageTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        rows = []
        for subject in range(10):
            for state, label in enumerate(("open", "closed")):
                image = self.root / f"{subject}_{label}.png"
                cv2.imwrite(str(image), np.full((8, 8), subject * 2 + state, dtype=np.uint8))
                rows.append({"image_path": str(image), "image_sha256": file_sha256(image),
                             "subject_id": str(subject), "label": label, "view": "unknown"})
        self.source = self.root / "source.csv"
        pd.DataFrame(rows).to_csv(self.source, index=False)
        self.manifest = self.root / "manifest.csv"

    def test_training_reads_only_the_eighty_percent_and_saves_fixed_final_epoch(self):
        prepare_manifest(self.source, self.manifest)
        frame = read_prepared_manifest(self.manifest)
        train_paths = set(frame.loc[frame.split == "train", "image_path"])
        test_paths = set(frame.loc[frame.split == "test", "image_path"])
        with patch("safedrive.models.eye_model.load_eye", wraps=load_eye) as load, contextlib.redirect_stdout(io.StringIO()):
            train(self.manifest, self.root / "model", epochs=2, seed=42)
        used_paths = {str(call.args[0]) for call in load.call_args_list}
        self.assertEqual(len(train_paths), 16)
        self.assertEqual(len(test_paths), 4)
        self.assertEqual(used_paths, train_paths)
        self.assertTrue(used_paths.isdisjoint(test_paths))
        self.assertEqual(len(load.call_args_list), 32)
        card = json.loads((self.root / "model/model-card.json").read_text())
        self.assertEqual(card["selected_epoch"], 2)
        self.assertNotIn("best_val_loss", card)
        history = json.loads((self.root / "model/history.json").read_text())
        self.assertTrue(all("val_loss" not in epoch for epoch in history))

    def test_previously_used_test_is_rejected_before_model_receives_images(self):
        prepare_manifest(self.source, self.manifest, prior_manifests=[self.source])
        with patch("safedrive.models.eye_model.load_eye") as load:
            with self.assertRaisesRegex(ValueError, "previously used"):
                train(self.manifest, self.root / "blocked", epochs=1, seed=42)
        load.assert_not_called()
        self.assertFalse((self.root / "blocked").exists())
        audit = json.loads(self.manifest.with_suffix(".split.json").read_text())
        self.assertEqual(audit["previously_used_test_images"], 4)

    def test_prior_subject_use_is_found_even_when_the_image_is_new(self):
        subject = next(iter(select_test_subjects({str(index): 2 for index in range(10)})))
        old = self.root / "old.png"
        cv2.imwrite(str(old), np.full((8, 8), 200, dtype=np.uint8))
        prior = self.root / "prior.csv"
        pd.DataFrame([{"image_path": str(old), "subject_id": subject,
                       "label": "open", "view": "unknown", "image_sha256": file_sha256(old)}]).to_csv(prior, index=False)
        prepare_manifest(self.source, self.manifest, prior_manifests=[prior])
        frame = pd.read_csv(self.manifest, dtype=str)
        marked = frame[frame.prior_usage == "used"]
        self.assertEqual(set(marked.subject_id), {subject})
        self.assertEqual(len(marked), 2)

    def test_source_without_hashes_rejects_a_renamed_duplicate(self):
        source = pd.read_csv(self.source, dtype=str).drop(columns="image_sha256")
        first = Path(source.image_path.iloc[0])
        duplicate = Path(source.image_path.iloc[-1])
        duplicate.write_bytes(first.read_bytes())
        source.to_csv(self.source, index=False)

        with self.assertRaisesRegex(ValueError, "Duplicate image content"):
            prepare_manifest(self.source, self.manifest)
        self.assertFalse(self.manifest.exists())

    def test_a_reserved_manifest_is_not_reshuffled_or_overwritten(self):
        prepare_manifest(self.source, self.manifest)
        before = file_sha256(self.manifest)
        with self.assertRaisesRegex(ValueError, "already exists"):
            prepare_manifest(self.source, self.manifest, seed=99)
        self.assertEqual(file_sha256(self.manifest), before)

    def test_new_training_rejects_the_historical_three_way_protocol(self):
        prepare_manifest(self.source, self.manifest)
        frame = pd.read_csv(self.manifest, dtype=str)
        subject = frame.loc[frame.split == "train", "subject_id"].iloc[0]
        frame.loc[frame.subject_id == subject, "split"] = "val"
        frame.to_csv(self.manifest, index=False)
        with self.assertRaisesRegex(ValueError, "80/20 train/test"):
            train(self.manifest, self.root / "legacy", epochs=1, seed=42)

    def test_archived_usage_is_detected_even_after_original_image_was_removed(self):
        source = pd.read_csv(self.source, dtype=str)
        test_subject = next(iter(select_test_subjects({str(index): 2 for index in range(10)})))
        image_hash = source.loc[source.subject_id == test_subject, "image_sha256"].iloc[0]
        source.drop(columns="image_sha256").to_csv(self.source, index=False)
        prior = self.root / "archived.csv"
        pd.DataFrame([{"image_path": "deleted.png", "subject_id": "another_old_id",
                       "label": "open", "view": "unknown", "image_sha256": image_hash}]).to_csv(prior, index=False)
        prepare_manifest(self.source, self.manifest, prior_manifests=[prior])
        frame = pd.read_csv(self.manifest, dtype=str)
        self.assertEqual(int((frame.prior_usage == "used").sum()), 1)
        with self.assertRaisesRegex(ValueError, "previously used"):
            read_prepared_manifest(self.manifest)

    def test_an_altered_seventy_thirty_manifest_cannot_be_trained(self):
        prepare_manifest(self.source, self.manifest)
        frame = pd.read_csv(self.manifest, dtype=str)
        subject = frame.loc[frame.split == "train", "subject_id"].iloc[0]
        frame.loc[frame.subject_id == subject, "split"] = "test"
        frame.to_csv(self.manifest, index=False)
        with self.assertRaisesRegex(ValueError, "80/20 requires"):
            train(self.manifest, self.root / "wrong_ratio", epochs=1, seed=42)
