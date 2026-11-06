"""A small PyTorch eye-state classifier, trained on labeled eye crops."""

import torch
from torch import nn


class EyeClassifier(nn.Module):
    def __init__(self, pool_size: int = 1):
        super().__init__()
        if pool_size not in (1, 4):
            raise ValueError("pool_size must be 1 or 4")
        self.layers = nn.Sequential(
            nn.Conv2d(1, 16, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
            nn.Conv2d(16, 32, 3, padding=1), nn.ReLU(), nn.MaxPool2d(2),
            nn.Conv2d(32, 64, 3, padding=1), nn.ReLU(),
            nn.AdaptiveAvgPool2d(pool_size), nn.Flatten(), nn.Linear(64 * pool_size * pool_size, 1),
        )

    def forward(self, image: torch.Tensor) -> torch.Tensor:
        # Logit for CLOSED. A sigmoid converts it to probability at inference time.
        return self.layers(image).squeeze(-1)


def load_eye(path):
    import cv2

    image = cv2.imread(str(path), cv2.IMREAD_GRAYSCALE)
    if image is None:
        raise ValueError(f"Cannot decode image: {path}")
    image = cv2.resize(image, (64, 64), interpolation=cv2.INTER_AREA)
    return torch.from_numpy(image.copy()).float().unsqueeze(0) / 255.0


class EyeDataset(torch.utils.data.Dataset):
    def __init__(self, frame):
        self.rows = frame.reset_index(drop=True)

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        row = self.rows.iloc[index]
        return load_eye(row.image_path), torch.tensor(float(row.label == "closed"))
