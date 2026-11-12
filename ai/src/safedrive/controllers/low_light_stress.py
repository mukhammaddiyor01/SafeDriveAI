"""Development-only synthetic low-light stress; never a real night accuracy claim."""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import time

import numpy as np
from PIL import Image

from safedrive.models.provenance import file_sha256


def select_development(manifest, per_group=4):
    if per_group < 1:
        raise ValueError('Require at least one image per group')
    with manifest.open(newline='') as stream:
        candidates = [row for row in csv.DictReader(stream) if row['split'] == 'train']
    selected = []
    for view in ('front', 'left', 'right'):
        for label in ('open', 'closed'):
            group = sorted((row for row in candidates if row['view'] == view and row['label'] == label),
                           key=lambda row: row['image_sha256'])
            if len(group) < per_group:
                raise ValueError(f'Insufficient development images: {view}/{label}')
            for row in group[:per_group]:
                path = (manifest.parent / row['image_path']).resolve()
                if file_sha256(path) != row['image_sha256']:
                    raise ValueError('Development image content does not match manifest')
                selected.append({**row, 'path': path})
    return selected


def tone_curve(rgb):
    """Reference of Android LowLightTone: fixed gamma .65, maximum gain three."""
    lut = np.array([min(math.floor(255 * (value / 255) ** .65 + .5), value * 3)
                    for value in range(256)], dtype=np.uint8)
    return lut[rgb]


def stress(manifest, model, output):
    import mediapipe as mp
    if output.exists():
        raise ValueError('Stress output already exists')
    selected = select_development(manifest)
    options = mp.tasks.vision.FaceLandmarkerOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=str(model)),
        num_faces=2, output_face_blendshapes=True)
    rows = []
    with mp.tasks.vision.FaceLandmarker.create_from_options(options) as detector:
        for row in selected:
            original = np.array(Image.open(row['path']).convert('RGB'))
            variants = {'original': original}
            for factor in (.30, .15):
                dark = np.floor(original.astype(float) * factor).astype(np.uint8)
                variants[f'dark_{factor}'] = dark
                variants[f'enhanced_{factor}'] = tone_curve(dark)
            variants['black_control'] = np.zeros_like(original)
            for name, image in variants.items():
                start = time.perf_counter()
                result = detector.detect(mp.Image(image_format=mp.ImageFormat.SRGB, data=image))
                found = len(result.face_landmarks) == 1
                eye_geometry = False
                if found:
                    face = result.face_landmarks[0]
                    for a, b in ((33, 133), (263, 362)):
                        width = math.hypot((face[a].x - face[b].x) * image.shape[1],
                                           (face[a].y - face[b].y) * image.shape[0])
                        eye_geometry = eye_geometry or width >= 10
                rows.append({'image_sha256': row['image_sha256'], 'view': row['view'],
                             'label': row['label'], 'variant': name, 'face_found': found,
                             'eye_geometry_available': found and eye_geometry,
                             'desktop_inference_ms': (time.perf_counter() - start) * 1000})
    variants = {}
    for name in sorted({row['variant'] for row in rows}):
        group = [row for row in rows if row['variant'] == name]
        variants[name] = {'samples': len(group), 'faces_found': sum(r['face_found'] for r in group),
                          'eye_geometry_available': sum(r['eye_geometry_available'] for r in group)}
    report = {'purpose': 'Artificial degradation of prior train/development images only',
              'not_training': True, 'not_real_night_accuracy': True,
              'manifest_sha256': file_sha256(manifest), 'model_sha256': file_sha256(model),
              'code_sha256': file_sha256(Path(__file__)), 'mediapipe_version': mp.__version__,
              'selected_hashes': [row['image_sha256'] for row in selected], 'variants': variants,
              'limitations': 'No ML Kit, Android CameraX, exposure noise, motion blur, FPS or real night validation. Eye geometry is not an open/closed prediction.'}
    output.mkdir(parents=True)
    (output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    with (output / 'observations.csv').open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader(); writer.writerows(rows)
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('manifest', type=Path)
    parser.add_argument('model', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    stress(args.manifest, args.model, args.output)
