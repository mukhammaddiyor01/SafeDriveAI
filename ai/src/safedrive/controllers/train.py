"""Train using only train subjects; choose checkpoint using validation subjects."""

import argparse
import json
from pathlib import Path


def train(manifest: Path, output: Path, epochs: int, seed: int):
    import pandas as pd
    import torch
    from torch import nn
    from torch.utils.data import DataLoader
    from safedrive.models.eye_model import EyeClassifier, EyeDataset

    if epochs < 1:
        raise ValueError("epochs must be positive")
    torch.manual_seed(seed)
    frame = pd.read_csv(manifest, dtype=str, keep_default_na=False)
    if not {"split", "image_path", "label", "subject_id", "view"} <= set(frame):
        raise ValueError("Run prepare first to produce a split manifest")
    if frame.groupby("subject_id").split.nunique().max() != 1:
        raise ValueError("Subject leakage across splits")
    if not set(frame.label) <= {"open", "closed"}:
        raise ValueError("Invalid labels")
    loaders = {}
    for split in ("train", "val"):
        subset = frame[frame.split == split]
        if set(subset.label) != {"open", "closed"}:
            raise ValueError(f"{split} must have both classes")
        loaders[split] = DataLoader(EyeDataset(subset), batch_size=32, shuffle=split == "train")
    model = EyeClassifier()
    optimizer = torch.optim.AdamW(model.parameters(), lr=0.001)
    criterion = nn.BCEWithLogitsLoss()
    best_loss = float("inf")
    history = []
    output.mkdir(parents=True, exist_ok=True)
    for epoch in range(epochs):
        row = {"epoch": epoch + 1}
        for split, loader in loaders.items():
            model.train(split == "train")
            total = 0.0
            with torch.set_grad_enabled(split == "train"):
                for images, labels in loader:
                    loss = criterion(model(images), labels)
                    if split == "train":
                        optimizer.zero_grad()
                        loss.backward()
                        optimizer.step()
                    total += loss.item() * len(images)
            row[f"{split}_loss"] = total / len(loader.dataset)
        if row["val_loss"] < best_loss:
            best_loss = row["val_loss"]
            torch.save(model.state_dict(), output / "eye_state.pt")
        history.append(row)
        print(json.dumps(row))
    (output / "history.json").write_text(json.dumps(history, indent=2))
    (output / "model-card.json").write_text(json.dumps({
        "input": "float32 NCHW [N,1,64,64], grayscale / 255",
        "output": "closed-eye logit; sigmoid then threshold validated on val subjects",
        "seed": seed, "manifest": str(manifest.resolve()),
        "status": "research only; evaluate held-out subjects and each camera view",
    }, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--output", type=Path, default=Path("artifacts/eye"))
    parser.add_argument("--epochs", type=int, default=15)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    train(args.manifest, args.output, args.epochs, args.seed)


if __name__ == "__main__":
    main()
