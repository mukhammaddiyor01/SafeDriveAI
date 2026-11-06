import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest

import cv2
import numpy as np
import pandas as pd

from safedrive.controllers.import_mrl import import_mrl


class MrlImportTest(unittest.TestCase):
    def test_duplicate_groups_are_excluded_and_provenance_is_recorded(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            images = root / "images"
            images.mkdir()
            for subject in range(1, 51):
                for state in (0, 1):
                    name = f"s{subject:04d}_00001_0_0_{state}_0_0_01.png"
                    cv2.imwrite(str(images / name), np.full((8, 8), subject * 2 + state, dtype=np.uint8))
            duplicate = images / "s0051_00001_0_0_1_0_0_01.png"
            duplicate.write_bytes((images / "s0001_00001_0_0_1_0_0_01.png").read_bytes())
            archive = root / "archive.zip"
            archive.write_bytes(b"synthetic archive fixture")
            output = root / "prepared"
            with contextlib.redirect_stdout(io.StringIO()):
                import_mrl(images, output, archive)
            frame = pd.read_csv(output / "manifest.csv")
            metadata = json.loads((output / "provenance.json").read_text())
            self.assertEqual(metadata["raw_images"], 101)
            self.assertEqual(metadata["excluded_duplicate_images"], 2)
            self.assertEqual(len(frame), 99)
            self.assertEqual(len(pd.read_csv(output / "excluded_duplicates.csv")), 2)
            self.assertEqual(set(frame.view), {"unknown"})
            self.assertFalse(frame.image_sha256.duplicated().any())
            self.assertTrue((frame.groupby("subject_id").split.nunique() == 1).all())
            with self.assertRaisesRegex(ValueError, "already exists"):
                import_mrl(images, output, archive)
