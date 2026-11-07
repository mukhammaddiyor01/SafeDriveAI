"""Prepare a local research pilot from the official RT-BENE subject archives."""

import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import tarfile
from tempfile import TemporaryDirectory

import pandas as pd

from safedrive.models.dataset import read_prepared_manifest
from safedrive.models.provenance import file_sha256, manifest_fingerprint


SOURCE = "https://zenodo.org/records/3685316"
PILOT_SUBJECTS = {7, 9, 12, 15, 16}
LABELS = {"0.0": "open", "1.0": "closed"}


def collect_subject(root, staging, row):
    subject, labels_file, left, right, category, _ = row
    subject = f"s{int(subject):03d}"
    split = {"training": "train", "validation": "test"}[category]
    with (root / labels_file).open(newline="") as stream, tarfile.open(
        root / f"{subject}_noglasses_eyes.tar"
    ) as archive:
        for filename, label in csv.reader(stream):
            if label == "0.5":
                continue
            match = re.fullmatch(r"(left|right)_\d+_rgb\.png", filename)
            if not match or label not in LABELS:
                raise ValueError(f"Invalid RT-BENE annotation for {subject}: {filename}")
            directory = left if match[1] == "left" else right
            member = archive.getmember(directory + filename)
            if not member.isfile():
                raise ValueError(f"Expected a regular image: {member.name}")
            relative = Path("images") / subject / filename
            image = staging / relative
            image.parent.mkdir(parents=True, exist_ok=True)
            with archive.extractfile(member) as source, image.open("xb") as destination:
                destination.write(source.read())
            yield {
                "image_path": relative.as_posix(), "subject_id": f"rt_bene_{subject}",
                "label": LABELS[label], "view": "unknown", "split": split,
                "image_sha256": file_sha256(image), "source": "RT-BENE",
            }


def prepare(root: Path, output: Path, prior: Path, seed: int = 42):
    if output.exists():
        raise ValueError(f"Output already exists: {output}")
    history = pd.read_csv(prior, dtype=str)
    historical_hashes = set(history["image_sha256"])
    historical_subjects = set(history["subject_id"])
    with (root / "subjects.csv").open(newline="") as stream:
        subjects = [
            row for row in csv.reader(stream)
            if int(row[0]) in PILOT_SUBJECTS and row[4] in {"training", "validation"}
        ]
    if {int(row[0]) for row in subjects} != PILOT_SUBJECTS:
        raise ValueError("Missing official pilot subject entries")
    output.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=output.parent) as temporary:
        staging = Path(temporary) / "prepared"
        staging.mkdir()
        frame = pd.DataFrame(
            row for subject in subjects for row in collect_subject(root, staging, subject)
        )
        counts_before = frame.split.value_counts().to_dict()
        duplicates = frame.image_sha256.duplicated(keep=False)
        historical = frame.image_sha256.isin(historical_hashes) | frame.subject_id.isin(historical_subjects)
        eligible = frame.loc[~(duplicates | historical)]
        train = eligible[eligible.split == "train"]
        test = eligible[eligible.split == "test"]
        test_count = min(len(test), len(train) // 4)

        def sample(group, count):
            ranks = group.image_sha256.map(
                lambda digest: hashlib.sha256(f"{seed}:{digest}".encode()).hexdigest()
            )
            return group.loc[ranks.sort_values().index[:count]]

        selected = pd.concat([sample(train, 4 * test_count), sample(test, test_count)])
        selected = selected.assign(prior_usage="unseen_in_supplied_history")
        manifest = staging / "manifest.csv"
        selected.to_csv(manifest, index=False)
        verified = read_prepared_manifest(manifest)
        report = {
            "source": SOURCE, "seed": seed, "before_filtering": counts_before,
            "prior_manifest_sha256": file_sha256(prior),
            "archives_sha256": {
                name: file_sha256(root / name)
                for name in (f"s{subject:03d}_noglasses_eyes.tar" for subject in sorted(PILOT_SUBJECTS))
            },
            "duplicate_rows_excluded": int(duplicates.sum()),
            "historical_rows_excluded": int(historical.sum()),
            "dataset_fingerprint": manifest_fingerprint(verified),
            "counts": {
                split: {"images": len(group), "subjects": group.subject_id.nunique(),
                        "closed": int((group.label == "closed").sum())}
                for split, group in selected.groupby("split")
            },
        }
        (staging / "split.json").write_text(json.dumps(report, indent=2) + "\n")
        staging.rename(output)
    print(json.dumps(report, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--prior-manifest", type=Path, required=True)
    args = parser.parse_args()
    prepare(args.root, args.output, args.prior_manifest)


if __name__ == "__main__":
    main()
