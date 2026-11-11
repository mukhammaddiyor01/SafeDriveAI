"""Extract frozen Face Landmarker features from labeled RT-GENE face images."""

import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import tarfile

import mediapipe as mp
import numpy as np
from PIL import Image
import io


def extract(source, output, model_path):
    output.mkdir(parents=True, exist_ok=False)
    rows = []
    coverage = {}
    source_hashes = {}
    options = mp.tasks.vision.FaceLandmarkerOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=str(model_path)),
        num_faces=2, output_face_blendshapes=True,
        output_facial_transformation_matrixes=True,
    )
    with mp.tasks.vision.FaceLandmarker.create_from_options(options) as detector:
        for subject in (7, 9, 10, 11, 12, 13):
            session = f"s{subject:03d}"
            annotations = source / f"{session}_blink_labels.csv"
            archive_path = source / f"{session}_noglasses.tar.prefix"
            labels = {}
            with annotations.open(newline="") as stream:
                for filename, label in csv.reader(stream):
                    frame = filename.split("_")[1]
                    labels.setdefault(frame, []).append(label)
            counts = {"eligible_frames": 0, "usable_frames": 0}
            limit = archive_path.stat().st_size
            with tarfile.open(archive_path) as archive:
                while archive.offset + 512 <= limit:
                    member = archive.next()
                    if member is None or member.offset_data + member.size > limit:
                        break
                    match = re.fullmatch(r"face_(\d+)_rgb\.png", Path(member.name).name)
                    if not member.isfile() or not match:
                        continue
                    frame = match[1]
                    states = labels.get(frame, [])
                    if len(states) != 2 or states[0] != states[1] or states[0] not in {"0.0", "1.0"}:
                        continue
                    counts["eligible_frames"] += 1
                    with archive.extractfile(member) as stream:
                        encoded = stream.read()
                    image = Image.open(io.BytesIO(encoded)).convert("RGB")
                    result = detector.detect(mp.Image(image_format=mp.ImageFormat.SRGB, data=np.array(image)))
                    if len(result.face_landmarks) != 1 or not result.face_blendshapes:
                        continue
                    face = result.face_landmarks[0]
                    rotation = np.array(result.facial_transformation_matrixes[0])[:3, :3]
                    u, _, v = np.linalg.svd(rotation)
                    rotation = u @ v
                    yaw = math.degrees(math.atan2(-rotation[2, 0], math.hypot(rotation[0, 0], rotation[1, 0])))
                    if abs(yaw) > 45:
                        continue
                    candidates = []
                    for side, indexes in (
                        ("Left", (263, 387, 385, 362, 380, 373)),
                        ("Right", (33, 160, 158, 133, 153, 144)),
                    ):
                        points = [(face[i].x * image.width, face[i].y * image.height) for i in indexes]
                        width = math.dist(points[0], points[3])
                        if width < 10:
                            continue
                        gap = (math.dist(points[1], points[5]) + math.dist(points[2], points[4])) / (2 * width)
                        if not 0 <= gap <= 0.65:
                            continue
                        blink = next(c.score for c in result.face_blendshapes[0] if c.category_name == "eyeBlink" + side)
                        candidates.append((width, blink, gap))
                    if not candidates:
                        continue
                    width, blink, gap = max(candidates)
                    eye_a, eye_b = face[33], face[263]
                    nose = face[1]
                    side_offset = nose.x - (eye_a.x + eye_b.x) / 2
                    view = "front" if abs(yaw) <= 18 else "left" if side_offset < 0 else "right"
                    relative = Path("images") / session / f"face_{frame}.png"
                    destination = output / relative
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    destination.write_bytes(encoded)
                    rows.append({
                        "image_path": relative.as_posix(), "image_sha256": hashlib.sha256(encoded).hexdigest(),
                        "subject_id": f"rt_bene_{session}", "label": "closed" if states[0] == "1.0" else "open",
                        "view": view, "yaw_estimate": yaw, "blink": blink, "gap": gap, "eye_width": width,
                        "split": "test" if subject in {11, 13} else "train",
                    })
                    counts["usable_frames"] += 1
            coverage[session] = counts
            for path in (annotations, archive_path):
                with path.open("rb") as stream:
                    source_hashes[path.name] = hashlib.file_digest(stream, "sha256").hexdigest()
            print(session, counts, flush=True)
    with (output / "features.csv").open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    (output / "extraction.json").write_text(json.dumps({
        "coverage": coverage, "sources_sha256": source_hashes,
        "model_sha256": hashlib.sha256(model_path.read_bytes()).hexdigest(),
        "mediapipe_version": mp.__version__,
        "pose_note": "Automatic Face Landmarker yaw estimate, not a manually measured camera angle",
    }, indent=2) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("model", type=Path)
    arguments = parser.parse_args()
    extract(arguments.source, arguments.output, arguments.model)
