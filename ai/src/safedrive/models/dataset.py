"""Subject-independent dataset splits. Never split adjacent video frames randomly."""

from pathlib import Path
import hashlib

from safedrive.models.provenance import file_sha256


REQUIRED_COLUMNS = {"image_path", "subject_id", "label", "view"}
LABELS = {"open", "closed"}
VIEWS = {"front", "left", "right", "up", "down", "unknown"}


def select_test_subjects(group_sizes, seed: int = 42) -> set[str]:
    """Reserve 20% (rounded to whole images) without splitting a driver."""
    subjects = sorted(group_sizes, key=lambda subject: hashlib.sha256(
        f"{seed}:{subject}".encode()).digest())
    if len(subjects) < 2:
        raise ValueError("At least two subjects are needed for an independent test set")
    total = sum(int(group_sizes[subject]) for subject in subjects)
    target = round(total * 0.20)
    # Bit n means a whole-subject subset containing exactly n images exists.
    reachable = [1]
    for subject in subjects:
        previous = reachable[-1]
        reachable.append(previous | (previous << int(group_sizes[subject])))
    if not 0 < target < total or not (reachable[-1] >> target) & 1:
        raise ValueError("Cannot make 80/20 with whole subjects; add independent subjects")
    remaining = target
    selected = set()
    for index in range(len(subjects) - 1, -1, -1):
        if not (reachable[index] >> remaining) & 1:
            selected.add(subjects[index])
            remaining -= int(group_sizes[subjects[index]])
    return selected


def read_manifest(path: Path):
    import pandas as pd

    frame = pd.read_csv(path, dtype=str, keep_default_na=False)
    missing = REQUIRED_COLUMNS - set(frame.columns)
    if missing:
        raise ValueError(f"Missing CSV columns: {', '.join(sorted(missing))}")
    if frame.empty or frame[list(REQUIRED_COLUMNS)].apply(lambda column: column.str.strip().eq("")).any().any():
        raise ValueError("Manifest must contain nonempty rows and fields")
    if not set(frame.label) <= LABELS:
        raise ValueError("label must be open or closed")
    if not set(frame.view) <= VIEWS:
        raise ValueError(f"view must be one of {sorted(VIEWS)}")
    frame["image_path"] = frame.image_path.map(
        lambda value: str((path.parent / value).resolve())
    )
    if frame.image_path.duplicated().any():
        raise ValueError("Duplicate images in manifest")
    for image_path in frame.image_path:
        if not Path(image_path).is_file():
            raise ValueError(f"Image does not exist: {image_path}")
    return frame


def validate_train_test_ratio(frame):
    expected = round(len(frame) * 0.20)
    actual = int((frame.split == "test").sum())
    if actual != expected:
        raise ValueError(f"80/20 requires {expected} test images, found {actual}; add whole subjects rather than mixing a driver across splits")


def read_prepared_manifest(path: Path):
    """Use the same data-integrity checks for training and held-out evaluation."""
    frame = read_manifest(path)
    if "split" not in frame or set(frame.split) not in ({"train", "test"}, {"train", "val", "test"}):
        raise ValueError("Run prepare first; split needs train and test (legacy val is optional)")
    if set(frame.split) == {"train", "test"}:
        validate_train_test_ratio(frame)
    if (frame.groupby("subject_id").split.nunique() > 1).any():
        raise ValueError("Subject leakage across splits")
    for split in sorted(set(frame.split)):
        if set(frame.loc[frame.split == split, "label"]) != LABELS:
            raise ValueError(f"{split} needs both open and closed eye examples")
    if "image_sha256" in frame:
        if not frame.image_sha256.str.fullmatch(r"[0-9a-f]{64}").all():
            raise ValueError("Invalid image_sha256")
        if frame.image_sha256.duplicated().any():
            raise ValueError("Duplicate image content in prepared manifest")
        for row in frame.itertuples():
            if file_sha256(Path(row.image_path)) != row.image_sha256:
                raise ValueError(f"Image content changed after import: {row.image_path}")
    if "prior_usage" in frame:
        if not set(frame.prior_usage) <= {"used", "unseen_in_supplied_history"}:
            raise ValueError("Invalid prior_usage audit labels")
        if (frame.loc[frame.split == "test", "prior_usage"] == "used").any():
            raise ValueError("Test contains previously used images; supply genuinely unused test data")
    return frame


def prepare_manifest(source: Path, destination: Path, seed: int = 42, *, prior_manifests=()):
    import json
    import pandas as pd

    if destination.exists():
        raise ValueError(f"Manifest already exists; do not reshuffle a reserved test set: {destination}")
    frame = read_manifest(source)
    test_subjects = select_test_subjects(frame.groupby("subject_id").size().to_dict(), seed)
    frame["split"] = frame.subject_id.map(lambda subject: "test" if subject in test_subjects else "train")
    validate_train_test_ratio(frame)
    for split in ("train", "test"):
        labels = set(frame.loc[frame.split == split, "label"])
        if labels != LABELS:
            raise ValueError(f"{split} needs both open and closed eye examples; add subjects")
    if prior_manifests:
        used_hashes, used_paths, used_subjects = set(), set(), set()
        for prior_path in prior_manifests:
            prior_path = Path(prior_path)
            prior = pd.read_csv(prior_path, dtype=str, keep_default_na=False)
            if not REQUIRED_COLUMNS <= set(prior.columns):
                raise ValueError(f"Invalid prior manifest: {prior_path}")
            prior["image_path"] = prior.image_path.map(lambda value: str((prior_path.parent / value).resolve()))
            used_paths.update(prior.image_path)
            used_subjects.update(prior.subject_id)
            if "image_sha256" in prior:
                used_hashes.update(prior.image_sha256)
        used = frame.image_path.isin(used_paths) | frame.subject_id.isin(used_subjects)
        if "image_sha256" in frame:
            used |= frame.image_sha256.isin(used_hashes)
        frame["prior_usage"] = used.map({True: "used", False: "unseen_in_supplied_history"})
    destination.parent.mkdir(parents=True, exist_ok=True)
    frame.to_csv(destination, index=False)
    counts = {str(key): int(value) for key, value in frame.split.value_counts().items()}
    report = {
        "policy": "80% train / 20% test, rounded to whole images, disjoint subjects",
        "seed": seed, "images": len(frame), "counts": counts,
        "test_fraction": counts["test"] / len(frame),
        "subjects": {split: int(frame.loc[frame.split == split, "subject_id"].nunique())
                     for split in ("train", "test")},
        "history_audited": bool(prior_manifests),
        "previously_used_test_images": int(((frame.split == "test") & (frame.prior_usage == "used")).sum())
            if prior_manifests else None,
        "history_note": "No prior manifest supplied; historical freshness is not established"
            if not prior_manifests else "Any prior image or subject use marks the sample used",
    }
    destination.with_suffix(".split.json").write_text(json.dumps(report, indent=2) + "\n")
    summary = frame.groupby(["split", "view", "label"]).size()
    return summary
