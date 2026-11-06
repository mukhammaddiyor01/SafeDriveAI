"""Train only on train subjects; the default 80/20 protocol saves the fixed final epoch."""

import argparse
from datetime import datetime, timezone
import json
import inspect
from pathlib import Path
import random
import time

from safedrive.models.dataset import read_prepared_manifest
from safedrive.models.provenance import file_sha256, manifest_fingerprint


def resolve_device(requested: str):
    import torch

    if requested not in {"auto", "cpu", "mps", "cuda"}:
        raise ValueError("device must be auto, cpu, mps, or cuda")
    if requested == "auto":
        requested = "cuda" if torch.cuda.is_available() else "mps" if torch.backends.mps.is_available() else "cpu"
    if requested == "mps" and not torch.backends.mps.is_available():
        raise ValueError("MPS is not available; select cpu or run with Metal access")
    if requested == "cuda" and not torch.cuda.is_available():
        raise ValueError("CUDA is not available")
    return torch.device(requested)


def epoch_loss(model, loader, criterion, device, optimizer=None):
    import torch

    model.train(optimizer is not None)
    total = 0.0
    with torch.set_grad_enabled(optimizer is not None):
        for images, labels in loader:
            images, labels = images.to(device), labels.to(device)
            loss = criterion(model(images), labels)
            if optimizer is not None:
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()
            total += loss.item() * len(images)
    return total / len(loader.dataset)


def train(manifest: Path, output: Path, epochs: int, seed: int, *,
          device: str = "cpu", batch_size: int = 32, threads: int = 4, pool_size: int = 1):
    import cv2
    import numpy as np
    import torch
    from torch import nn
    from torch.utils.data import DataLoader
    from safedrive.models.eye_model import EyeClassifier, EyeDataset

    if epochs < 1 or batch_size < 1 or threads < 1:
        raise ValueError("epochs, batch_size, and threads must be positive")
    if output.exists() and any(output.iterdir()):
        raise ValueError(f"Output is not empty: {output}")
    selected_device = resolve_device(device)
    frame = read_prepared_manifest(manifest)
    if set(frame.split) != {"train", "test"}:
        raise ValueError("New training requires an 80/20 train/test manifest; legacy validation splits are historical only")
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.set_num_threads(threads)
    cv2.setNumThreads(1)
    loader = DataLoader(EyeDataset(frame[frame.split == "train"]), batch_size=batch_size,
                        shuffle=True, num_workers=0,
                        generator=torch.Generator().manual_seed(seed))
    model = EyeClassifier(pool_size=pool_size).to(selected_device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=0.001)
    criterion = nn.BCEWithLogitsLoss()
    history = []
    output.mkdir(parents=True, exist_ok=True)
    card = {
        "input": "float32 NCHW [N,1,64,64], OpenCV grayscale / 255, INTER_AREA resize",
        "output": "closed-eye logit; choose sigmoid threshold before test inference",
        "architecture": {"name": "EyeClassifier", "pool_size": pool_size},
        "seed": seed, "manifest": str(manifest.resolve()),
        "manifest_sha256": file_sha256(manifest), "dataset_fingerprint": manifest_fingerprint(frame),
        "training_code_sha256": file_sha256(Path(__file__)),
        "model_code_sha256": file_sha256(Path(inspect.getfile(EyeClassifier))),
        "device": str(selected_device), "torch_version": str(torch.__version__),
        "batch_size": batch_size, "threads": threads, "requested_epochs": epochs,
        "learning_rate": 0.001, "optimizer": "AdamW", "loss": "BCEWithLogitsLoss",
        "selection": "fixed final epoch; no test inference or test-driven selection during training",
        "split_policy": "80/20 train/test",
        "started_at_utc": datetime.now(timezone.utc).isoformat(), "completed_epochs": 0,
        "status": "training in progress; research only",
        "partition_counts": {str(key): int(value) for key, value in frame.split.value_counts().items()},
    }
    (output / "model-card.json").write_text(json.dumps(card, indent=2) + "\n")
    print(json.dumps({"device": str(selected_device), "batch_size": batch_size,
                      "epochs": epochs, "dataset_fingerprint": card["dataset_fingerprint"]}), flush=True)
    for epoch in range(epochs):
        started = time.perf_counter()
        row = {"epoch": epoch + 1}
        row["train_loss"] = epoch_loss(model, loader, criterion, selected_device, optimizer)
        row["seconds"] = round(time.perf_counter() - started, 3)
        if epoch + 1 == epochs:
            torch.save({key: value.detach().cpu() for key, value in model.state_dict().items()}, output / "eye_state.pt")
            card.update(selected_epoch=epoch + 1, checkpoint_sha256=file_sha256(output / "eye_state.pt"))
        history.append(row)
        card["completed_epochs"] = epoch + 1
        (output / "history.json").write_text(json.dumps(history, indent=2) + "\n")
        (output / "model-card.json").write_text(json.dumps(card, indent=2) + "\n")
        print(json.dumps(row), flush=True)
    card.update(status="training complete; held-out and phone validation still required",
                finished_at_utc=datetime.now(timezone.utc).isoformat())
    (output / "model-card.json").write_text(json.dumps(card, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--output", type=Path, default=Path("artifacts/eye"))
    parser.add_argument("--epochs", type=int, default=15)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--device", choices=("auto", "cpu", "mps", "cuda"), default="auto")
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--pool-size", type=int, choices=(1, 4), default=1)
    args = parser.parse_args()
    train(args.manifest, args.output, args.epochs, args.seed,
          device=args.device, batch_size=args.batch_size, threads=args.threads, pool_size=args.pool_size)


if __name__ == "__main__":
    main()
