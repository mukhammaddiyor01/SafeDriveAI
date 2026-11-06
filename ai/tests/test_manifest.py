import tempfile
import unittest
from pathlib import Path

import pandas as pd

from safedrive.models.dataset import read_prepared_manifest


class PreparedManifestTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.path = self.root / "manifest.csv"
        self.rows = []
        for index, split in enumerate(("train", "val", "test")):
            for label in ("open", "closed"):
                image = self.root / f"{split}_{label}.png"
                image.write_bytes(f"fixture {split} {label}".encode())
                self.rows.append({"image_path": image.name, "subject_id": str(index),
                                  "label": label, "view": "unknown", "split": split})

    def read(self):
        pd.DataFrame(self.rows).to_csv(self.path, index=False)
        return read_prepared_manifest(self.path)

    def test_unknown_camera_view_is_preserved_without_guessing(self):
        frame = self.read()
        self.assertEqual(set(frame.view), {"unknown"})
        self.assertTrue(all(Path(path).is_absolute() for path in frame.image_path))

    def test_same_subject_in_two_splits_is_rejected(self):
        self.rows[2]["subject_id"] = self.rows[0]["subject_id"]
        with self.assertRaisesRegex(ValueError, "Subject leakage"):
            self.read()

    def test_typo_in_split_and_missing_partition_are_rejected(self):
        self.rows[-1]["split"] = "testing"
        with self.assertRaisesRegex(ValueError, "split"):
            self.read()
        self.rows = self.rows[:4]
        with self.assertRaisesRegex(ValueError, "test"):
            self.read()

    def test_one_class_partition_is_rejected(self):
        self.rows[-1]["label"] = "open"
        with self.assertRaisesRegex(ValueError, "both"):
            self.read()

    def test_repeated_image_contents_are_rejected_when_hashes_are_available(self):
        for index, row in enumerate(self.rows):
            row["image_sha256"] = f"{index:064x}"
        self.rows[-1]["image_sha256"] = self.rows[0]["image_sha256"]
        with self.assertRaisesRegex(ValueError, "Duplicate image content"):
            self.read()
