"""Synthetic calibration tests check isolation, not real-world eye accuracy."""
import json
from pathlib import Path
import shutil

import cv2
import numpy as np
import pandas as pd

from safedrive.controllers.calibrate_eye_state import calibrate
from safedrive.models.provenance import file_sha256


def test_test_labels_do_not_change_trained_coefficients(tmp_path):
    root = tmp_path / 'first'
    root.mkdir()
    rows = []
    for subject in range(5):
        for view_index, view in enumerate(('left', 'front', 'right')):
            for closed in (False, True):
                name = f'{subject}_{view}_{closed}.png'
                level = 10 + subject * 12 + view_index * 2 + int(closed)
                cv2.imwrite(str(root / name), np.full((32, 32), level, dtype=np.uint8))
                rows.append(dict(image_path=name, image_sha256=file_sha256(root / name),
                                 subject_id=f'driver{subject}', view=view,
                                 label='closed' if closed else 'open',
                                 split='test' if subject == 4 else 'train',
                                 blink=0.8 if closed else 0.1, gap=0.06 if closed else 0.25))
    pd.DataFrame(rows).to_csv(root / 'features.csv', index=False)
    other = tmp_path / 'second'
    shutil.copytree(root, other)
    changed = pd.read_csv(other / 'features.csv')
    test = changed.split == 'test'
    changed.loc[test, 'label'] = changed.loc[test, 'label'].map({'open': 'closed', 'closed': 'open'})
    changed.to_csv(other / 'features.csv', index=False)
    prior = tmp_path / 'history.csv'
    pd.DataFrame([dict(subject_id='previous', image_sha256='0' * 64)]).to_csv(prior, index=False)
    calibrate(root, tmp_path / 'model1', [prior])
    calibrate(other, tmp_path / 'model2', [prior])
    first = json.loads((tmp_path / 'model1/report.json').read_text())
    second = json.loads((tmp_path / 'model2/report.json').read_text())
    assert first['coefficients'] == second['coefficients']
    assert first['counts'] == {'test': 6, 'train': 24}
    assert not first['accepted_for_integration']  # Insufficient held-out closed examples.
    manifest = pd.read_csv(root / 'manifest.csv')
    assert manifest.groupby('subject_id').split.nunique().max() == 1
    assert first['metrics'] != second['metrics']
