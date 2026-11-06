"""Convert an author's unpacked MRL archive into a traceable eye-crop manifest."""

import argparse
from dataclasses import asdict
from datetime import datetime, timezone
import json
import os
from pathlib import Path

from safedrive.models.dataset import prepare_manifest
from safedrive.models.provenance import file_sha256
from safedrive.models.mrl import ARCHIVE_URL, SOURCE_URL, parse_filename


def import_mrl(image_root: Path, output: Path, archive: Path, seed: int = 42):
    import pandas as pd

    if not image_root.is_dir() or not archive.is_file():
        raise ValueError("Provide the extracted MRL image directory and its original archive")
    if output.exists():
        raise ValueError(f"Output already exists: {output}")
    output.mkdir(parents=True)
    rows = []
    for image in sorted(image_root.rglob("*.png")):
        labels = asdict(parse_filename(image))
        rows.append({
            "image_path": os.path.relpath(image.resolve(), output.resolve()),
            **labels, "view": "unknown", "source": "MRL Eye 2018",
            "image_sha256": file_sha256(image),
        })
    if not rows:
        raise ValueError("No MRL PNG images found")
    frame = pd.DataFrame(rows)
    raw_images = len(frame)
    repeated = frame.image_sha256.duplicated(keep=False)
    frame.loc[repeated].to_csv(output / "excluded_duplicates.csv", index=False)
    # Exclude every copy, including conflicting labels or copies assigned to different people.
    frame = frame.loc[~repeated].copy()
    frame.to_csv(output / "source.csv", index=False)
    summary = prepare_manifest(output / "source.csv", output / "manifest.csv", seed)
    prepared = pd.read_csv(output / "manifest.csv", dtype=str)
    counts = prepared.groupby(["split", "label"]).size().unstack(fill_value=0)
    counts["subjects"] = prepared.groupby("split").subject_id.nunique()
    counts.to_csv(output / "split_summary.csv")
    provenance = {
        "dataset": "MRL Eye 2018", "source_url": SOURCE_URL, "archive_url": ARCHIVE_URL,
        "archive_sha256": file_sha256(archive),
        "manifest_sha256": file_sha256(output / "manifest.csv"),
        "imported_at_utc": datetime.now(timezone.utc).isoformat(),
        "images": len(frame), "subjects": int(frame.subject_id.nunique()), "seed": seed,
        "raw_images": raw_images, "excluded_duplicate_images": int(repeated.sum()),
        "deduplication": "Exclude every member of each repeated file-byte SHA-256 group before splitting",
        "eye_state_mapping": {"0": "closed", "1": "open"},
        "view": "unknown: author filenames do not annotate head/camera angle",
        "modality": "infrared eye crops; RGB phone performance is not established",
        "distribution": "Local research only; no redistribution or commercial permission assumed",
    }
    (output / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    print(counts.to_string())
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image_root", type=Path)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("data/mrl/prepared"))
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    import_mrl(args.image_root, args.output, args.archive, args.seed)


if __name__ == "__main__":
    main()
