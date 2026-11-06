import unittest
import pandas as pd
from safedrive.models.provenance import manifest_fingerprint


class ManifestIdentityTest(unittest.TestCase):
    def test_relocation_and_row_order_do_not_change_hashed_dataset_identity(self):
        original = pd.DataFrame([
            {"image_path": "/a/one.png", "image_sha256": "a" * 64, "label": "open", "split": "train"},
            {"image_path": "/a/two.png", "image_sha256": "b" * 64, "label": "closed", "split": "val"},
        ])
        moved = original.iloc[::-1].copy()
        moved["image_path"] = ["/b/two.png", "/b/one.png"]
        self.assertEqual(manifest_fingerprint(original), manifest_fingerprint(moved))
        moved.loc[moved.index[0], "label"] = "open"
        self.assertNotEqual(manifest_fingerprint(original), manifest_fingerprint(moved))
