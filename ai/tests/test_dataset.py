import unittest
from safedrive.models.dataset import subject_split


class SubjectSplitTest(unittest.TestCase):
    def test_all_frames_of_subject_share_one_split(self):
        results = [subject_split("driver_001") for _ in range(100)]
        self.assertEqual(len(set(results)), 1)

    def test_splits_cover_all_partitions(self):
        results = {subject_split(str(index)) for index in range(100)}
        self.assertEqual(results, {"train", "val", "test"})

    def test_seed_changes_partition_for_some_subjects(self):
        self.assertTrue(any(subject_split(str(i), 1) != subject_split(str(i), 2) for i in range(30)))


if __name__ == "__main__":
    unittest.main()
