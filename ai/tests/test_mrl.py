from pathlib import Path
import unittest

from safedrive.models.mrl import parse_filename


class MrlLabelsTest(unittest.TestCase):
    def test_eye_state_is_the_fifth_field_not_glasses_or_reflections(self):
        closed = parse_filename(Path("s0001_00001_0_1_0_2_1_01.png"))
        opened = parse_filename(Path("s0001_00002_0_0_1_0_0_03.png"))
        self.assertEqual(closed.label, "closed")
        self.assertEqual(opened.label, "open")
        self.assertEqual(closed.subject_id, opened.subject_id)
        self.assertEqual(closed.subject_id, "mrl_s0001")
        self.assertEqual(closed.glasses, "1")
        self.assertEqual(closed.reflections, "2")
        self.assertEqual(opened.sensor, "03")

    def test_malformed_names_and_unknown_labels_fail(self):
        for name in ("open.png", "s0001_1_0_0_2_0_0_01.png",
                     "s0001_1_0_0_1_3_0_01.png", "s0001_1_0_0_1_0_0_99.png"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                parse_filename(Path(name))
