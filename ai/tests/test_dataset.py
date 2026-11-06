import unittest
from safedrive.models.dataset import select_test_subjects


class SubjectSplitTest(unittest.TestCase):
    def test_equal_groups_reserve_exactly_twenty_percent(self):
        sizes = {f"driver_{index}": 10 for index in range(50)}
        selected = select_test_subjects(sizes)
        self.assertEqual(sum(sizes[subject] for subject in selected), 100)
        self.assertEqual(len(selected), 10)

    def test_different_group_sizes_use_image_count_not_person_count(self):
        sizes = {"a": 71, "b": 9, "c": 11, "d": 9}
        selected = select_test_subjects(sizes)
        self.assertEqual(sum(sizes[subject] for subject in selected), 20)

    def test_indivisible_groups_must_still_meet_the_rounded_ratio(self):
        sizes = {"a": 45, "b": 35, "c": 20}
        self.assertEqual(select_test_subjects(sizes), {"c"})
        sizes = {"a": 6, "b": 4}
        with self.assertRaisesRegex(ValueError, "Cannot make 80/20"):
            select_test_subjects(sizes)

    def test_order_independent_and_seeded(self):
        sizes = {str(index): 2 for index in range(30)}
        self.assertEqual(select_test_subjects(sizes), select_test_subjects(dict(reversed(list(sizes.items())))))
        self.assertNotEqual(select_test_subjects(sizes, 1), select_test_subjects(sizes, 2))

    def test_one_driver_cannot_be_split_between_train_and_test(self):
        with self.assertRaisesRegex(ValueError, "two subjects"):
            select_test_subjects({"only_driver": 100})
