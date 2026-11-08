import pandas as pd
import pytest

from safedrive.models.dataset import prepare_manifest, read_prepared_manifest


def test_center_mount_requires_coverage_in_each_split(tmp_path):
    rows = []
    for driver in range(5):
        for mount in ("center_low", "center_high"):
            for label, posture, gaze in (
                ("open", "upright", "road"),
                ("closed", "upright", "unknown"),
                ("open", "upright", "navigation"),
                ("open", "down", "unknown"),
            ):
                image = tmp_path / f"{len(rows)}.png"
                image.write_bytes(image.name.encode())
                rows.append(dict(image_path=image.name, subject_id=str(driver),
                                 label=label, view="left", camera_mount=mount,
                                 head_posture=posture, gaze_target=gaze))

    source = tmp_path / "source.csv"
    output = tmp_path / "prepared.csv"
    pd.DataFrame(rows).to_csv(source, index=False)
    prepare_manifest(source, output)
    prepared = read_prepared_manifest(output)
    assert prepared.split.value_counts().to_dict() == {"train": 32, "test": 8}
    assert prepared.groupby("subject_id").split.nunique().max() == 1

    navigation = (prepared.split == "test") & (prepared.camera_mount == "center_high") & (
        prepared.gaze_target == "navigation"
    )
    prepared.loc[navigation, "gaze_target"] = "road"
    prepared.to_csv(output, index=False)
    with pytest.raises(ValueError, match="test/center_high missing scenarios"):
        read_prepared_manifest(output)
