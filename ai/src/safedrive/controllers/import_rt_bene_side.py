"""Join RT-BENE eye-state labels to RT-GENE face crops for an 80/20 side-view pilot."""

import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import tarfile
from tempfile import TemporaryDirectory

import cv2
import numpy as np
import pandas as pd

from safedrive.controllers.import_rt_bene import collect_subject
from safedrive.models.dataset import read_prepared_manifest
from safedrive.models.provenance import file_sha256, manifest_fingerprint


TRAIN_SUBJECTS = {7, 12, 15, 16}
TEST_SUBJECTS = {3, 4}
SIDE_THRESHOLD = 0.18
SEED = 42
FACE_PREFIX_BYTES = 48 * 1024 * 1024


def side_views(archive_path, detector, frame_ids, include_front=False):
    """Estimate image-left/right head turn after projecting onto the eye axis."""
    views = {}
    if archive_path.stat().st_size != FACE_PREFIX_BYTES:
        raise ValueError(f"Expected the fixed 48 MiB RT-GENE prefix: {archive_path}")
    with tarfile.open(archive_path) as archive:
        while archive.offset + 512 <= FACE_PREFIX_BYTES:
            member = archive.next()
            if member is None or member.offset_data + member.size > FACE_PREFIX_BYTES:
                break
            match = re.fullmatch(r"face_(\d+)_rgb\.png", Path(member.name).name)
            if not member.isfile() or not match or match[1] not in frame_ids:
                continue
            with archive.extractfile(member) as stream:
                encoded = np.frombuffer(stream.read(), dtype=np.uint8)
            image = cv2.imdecode(encoded, cv2.IMREAD_COLOR)
            if image is None:
                continue
            height, width = image.shape[:2]
            detector.setInputSize((width, height))
            _, faces = detector.detect(image)
            if faces is None or len(faces) != 1:
                continue
            eye_a, eye_b, nose = faces[0, 4:10].reshape(3, 2)
            left, right = sorted((eye_a, eye_b), key=lambda point: point[0])
            axis = right - left
            distance_squared = float(axis @ axis)
            if distance_squared < 64:
                continue
            offset = float((nose - (left + right) / 2) @ axis) / distance_squared
            if abs(offset) >= SIDE_THRESHOLD:
                views[match[1]] = "left" if offset < 0 else "right"
            elif include_front:
                views[match[1]] = "front"
    return views


def prepare(blink_root, face_root, weights, output, prior_manifests, *,
            train_subjects=TRAIN_SUBJECTS, test_subjects=TEST_SUBJECTS, include_front=False):
    if not train_subjects or not test_subjects or train_subjects & test_subjects:
        raise ValueError("Supply disjoint train and test subjects")
    # RT-GENE sessions 014/002 and 015/005 record the same participants.
    aliases = {14: 2, 15: 5}
    if {aliases.get(s, s) for s in train_subjects} & {aliases.get(s, s) for s in test_subjects}:
        raise ValueError("Repeated RT-GENE participant crosses the split")
    if output.exists():
        raise ValueError(f"Output already exists: {output}")
    history = pd.concat([pd.read_csv(path, dtype=str) for path in prior_manifests])
    cv2.setNumThreads(1)
    detector = cv2.FaceDetectorYN.create(str(weights), "", (320, 320), 0.85)
    output.parent.mkdir(parents=True, exist_ok=True)
    with (blink_root / "subjects.csv").open(newline="") as stream:
        subjects = [row for row in csv.reader(stream) if int(row[0]) in train_subjects | test_subjects]
    if {int(row[0]) for row in subjects} != train_subjects | test_subjects:
        raise ValueError("Missing official RT-BENE subject rows")
    with TemporaryDirectory(dir=output.parent) as temporary:
        staging = Path(temporary) / "prepared"
        staging.mkdir()
        candidates = []
        inputs = {}
        coverage = {}
        for row in subjects:
            subject = f"s{int(row[0]):03d}"
            eyes = pd.DataFrame(collect_subject(blink_root, staging, row))
            frame_ids = eyes.image_path.str.extract(r"_(\d+)_rgb\.png$")[0]
            face_archive = face_root / f"{subject}_noglasses.tar.prefix"
            views = side_views(face_archive, detector, set(frame_ids), include_front)
            eyes["view"] = frame_ids.map(views)
            eyes["split"] = "test" if int(row[0]) in test_subjects else "train"
            candidates.append(eyes.dropna(subset=["view"]))
            coverage[subject] = {"annotated_frames": frame_ids.nunique(), "side_frames": len(views)}
            for path in (face_archive, blink_root / row[1], blink_root / f"{subject}_noglasses_eyes.tar"):
                inputs[path.name] = file_sha256(path)
            print(subject, coverage[subject], flush=True)

        frame = pd.concat(candidates, ignore_index=True)
        previous = frame.image_sha256.isin(history.image_sha256) | frame.subject_id.isin(history.subject_id)
        duplicate = frame.image_sha256.duplicated(keep=False)
        eligible = frame.loc[~duplicate & ~((frame.split == "test") & previous)].copy()
        eligible["prior_usage"] = previous.map({True: "used", False: "unseen_in_supplied_history"})
        eligible["rank"] = eligible.image_sha256.map(
            lambda digest: hashlib.sha256(f"{SEED}:{digest}".encode()).hexdigest()
        )
        selected = []
        views_to_select = ("left", "front", "right") if include_front else ("left", "right")
        for view in views_to_select:
            rows = eligible[eligible.view == view].sort_values("rank")
            train, test = rows[rows.split == "train"], rows[rows.split == "test"]
            test_count = min(len(test), len(train) // 4)
            if not test_count:
                raise ValueError(f"No independent 80/20 side-view examples for {view}")
            selected.extend((train.head(4 * test_count), test.head(test_count)))
        manifest = staging / "manifest.csv"
        pd.concat(selected).drop(columns="rank").to_csv(manifest, index=False)
        verified = read_prepared_manifest(manifest)
        if include_front:
            for (split, view), group in verified.groupby(["split", "view"]):
                if set(group.label) != {"open", "closed"}:
                    raise ValueError(f"{split}/{view} needs both eye states before training")
        report = {
            "seed": SEED, "train_subjects": sorted(train_subjects),
            "test_subjects": sorted(test_subjects), "include_front": include_front,
            "side_threshold": SIDE_THRESHOLD, "face_prefix_bytes": FACE_PREFIX_BYTES,
            "yunet_sha256": file_sha256(weights), "source_sha256": inputs,
            "prior_manifest_sha256": {str(path): file_sha256(path) for path in prior_manifests},
            "annotated_and_selected_frame_counts": coverage,
            "duplicate_rows_excluded": int(duplicate.sum()),
            "prior_test_rows_excluded": int(((frame.split == "test") & previous).sum()),
            "dataset_fingerprint": manifest_fingerprint(verified),
            "counts": verified.groupby(["split", "view", "label"]).size().rename("images").reset_index().to_dict("records"),
            "subject_counts": verified.groupby("split").subject_id.nunique().to_dict(),
        }
        (staging / "split.json").write_text(json.dumps(report, indent=2) + "\n")
        staging.rename(output)
        print(json.dumps(report, indent=2), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("blink_root", type=Path)
    parser.add_argument("face_root", type=Path)
    parser.add_argument("weights", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--prior-manifest", type=Path, action="append", required=True)
    parser.add_argument("--train-subjects", type=int, nargs="+", default=sorted(TRAIN_SUBJECTS))
    parser.add_argument("--test-subjects", type=int, nargs="+", default=sorted(TEST_SUBJECTS))
    parser.add_argument("--include-front", action="store_true")
    args = parser.parse_args()
    prepare(args.blink_root, args.face_root, args.weights, args.output, args.prior_manifest,
            train_subjects=set(args.train_subjects), test_subjects=set(args.test_subjects),
            include_front=args.include_front)


if __name__ == "__main__":
    main()
