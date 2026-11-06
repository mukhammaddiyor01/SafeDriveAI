"""Subject-independent dataset splits. Never split adjacent video frames randomly."""

from pathlib import Path
import hashlib


REQUIRED_COLUMNS = {"image_path", "subject_id", "label", "view"}
LABELS = {"open", "closed"}
VIEWS = {"front", "left", "right", "up", "down", "unknown"}


def subject_split(subject_id: str, seed: int = 42) -> str:
    digest = hashlib.sha256(f"{seed}:{subject_id}".encode()).digest()
    bucket = int.from_bytes(digest[:8], "big") % 100
    return "train" if bucket < 70 else "val" if bucket < 85 else "test"


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


def read_prepared_manifest(path: Path):
    """Use the same data-integrity checks for training and held-out evaluation."""
    frame = read_manifest(path)
    if "split" not in frame or not set(frame.split) <= {"train", "val", "test"}:
        raise ValueError("Run prepare first; split must be train, val, or test")
    if (frame.groupby("subject_id").split.nunique() > 1).any():
        raise ValueError("Subject leakage across splits")
    for split in ("train", "val", "test"):
        if set(frame.loc[frame.split == split, "label"]) != LABELS:
            raise ValueError(f"{split} needs both open and closed eye examples")
    if "image_sha256" in frame:
        if not frame.image_sha256.str.fullmatch(r"[0-9a-f]{64}").all():
            raise ValueError("Invalid image_sha256")
        if frame.image_sha256.duplicated().any():
            raise ValueError("Duplicate image content in prepared manifest")
    return frame


def prepare_manifest(source: Path, destination: Path, seed: int = 42):
    frame = read_manifest(source)
    frame["split"] = frame.subject_id.map(lambda subject: subject_split(subject, seed))
    for split in ("train", "val", "test"):
        labels = set(frame.loc[frame.split == split, "label"])
        if labels != LABELS:
            raise ValueError(f"{split} needs both open and closed eye examples; add subjects")
    destination.parent.mkdir(parents=True, exist_ok=True)
    frame.to_csv(destination, index=False)
    summary = frame.groupby(["split", "view", "label"]).size()
    return summary
