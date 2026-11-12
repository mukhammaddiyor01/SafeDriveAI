import csv
from pathlib import Path

import numpy as np
from PIL import Image
import pytest

from safedrive.controllers.low_light_stress import select_development, tone_curve
from safedrive.models.provenance import file_sha256


def test_stress_reserves_train_images_and_rejects_changed_content(tmp_path):
    rows = []
    for index, (view, label) in enumerate((v, l) for v in ('front', 'left', 'right') for l in ('open', 'closed')):
        for split in ('train', 'test'):
            image = tmp_path / f'{index}_{split}.png'
            Image.new('RGB', (20, 20), (index * 20, 10 if split == 'train' else 200, 30)).save(image)
            rows.append(dict(image_path=image.name, image_sha256=file_sha256(image),
                             view=view, label=label, split=split))
    manifest = tmp_path / 'manifest.csv'
    with manifest.open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0])); writer.writeheader(); writer.writerows(rows)
    selected = select_development(manifest, 1)
    assert len(selected) == 6
    assert all(row['split'] == 'train' for row in selected)
    selected[0]['path'].write_bytes(b'changed')
    with pytest.raises(ValueError, match='content'):
        select_development(manifest, 1)


def test_tone_preserves_black_detail_order_and_caps_amplification():
    original = np.arange(256, dtype=np.uint8).reshape(16, 16)
    corrected = tone_curve(original)
    assert corrected[0, 0] == 0
    assert corrected[-1, -1] == 255
    assert (np.diff(corrected.astype(int).reshape(-1)) >= 0).all()
    assert (corrected.astype(int) <= np.minimum(original.astype(int) * 3, 255)).all()
    assert (corrected[1:5] > original[1:5]).all()
