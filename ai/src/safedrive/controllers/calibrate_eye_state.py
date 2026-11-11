"""Fit a small PyTorch calibration using only the reserved train partition."""

import argparse
import hashlib
import json
from pathlib import Path

import pandas as pd
import torch
from torch import nn

from safedrive.controllers.evaluate import classification_metrics
from safedrive.models.dataset import read_prepared_manifest
from safedrive.models.provenance import file_sha256, manifest_fingerprint


def calibrate(root, output, prior_manifests):
    if output.exists():
        raise ValueError("Calibration output already exists")
    manifest = root / "manifest.csv"
    if manifest.exists():
        raise ValueError("Reserved manifest already exists")
    frame = pd.read_csv(root / "features.csv")
    history = pd.concat([pd.read_csv(path, dtype=str) for path in prior_manifests])
    historical = frame.subject_id.isin(history.subject_id) | frame.image_sha256.isin(history.image_sha256)
    duplicate = frame.image_sha256.duplicated(keep=False)
    eligible = frame.loc[~duplicate & ~((frame.split == "test") & historical)].copy()
    eligible["prior_usage"] = historical.map({True: "used", False: "unseen_in_supplied_history"})
    eligible["rank"] = eligible.image_sha256.map(lambda digest: hashlib.sha256(f"42:{digest}".encode()).hexdigest())
    selected = []
    for view in ("left", "front", "right"):
        group = eligible[eligible.view == view].sort_values("rank")
        train, test = group[group.split == "train"], group[group.split == "test"]
        count = min(len(test), len(train) // 4)
        if count == 0:
            raise ValueError(f"No independent samples for {view}")
        selected.extend((train.head(count * 4), test.head(count)))
    frame = pd.concat(selected).drop(columns="rank")
    frame.to_csv(manifest, index=False)
    verified = read_prepared_manifest(manifest)
    output.mkdir(parents=True)
    split = {
        "fingerprint": manifest_fingerprint(verified), "prior_manifests": {str(p): file_sha256(p) for p in prior_manifests},
        "duplicate_rows_excluded": int(duplicate.sum()),
        "historical_test_rows_excluded": int(((frame.split == "test") & historical.reindex(frame.index)).sum()),
        "counts": frame.groupby(["split", "view", "label"]).size().rename("images").reset_index().to_dict("records"),
    }
    (root / "split.json").write_text(json.dumps(split, indent=2) + "\n")
    torch.manual_seed(42)
    torch.set_num_threads(4)
    train = frame[frame.split == "train"]
    x = torch.tensor(train[["blink", "gap"]].to_numpy(), dtype=torch.float32)
    y = torch.tensor((train.label == "closed").to_numpy(), dtype=torch.float32)
    mean, std = x.mean(0), x.std(0).clamp_min(1e-6)
    model = nn.Linear(2, 1)
    optimizer = torch.optim.Adam(model.parameters(), lr=0.025)
    positive_weight = (len(y) - y.sum()) / y.sum()
    criterion = nn.BCEWithLogitsLoss(pos_weight=positive_weight)
    for epoch in range(400):
        optimizer.zero_grad()
        loss = criterion(model((x - mean) / std).squeeze(1), y)
        loss.backward()
        optimizer.step()
    weights = model.weight.detach().squeeze(0) / std
    bias = model.bias.detach().item() - (weights * mean).sum().item()
    coefficients = {"blink_weight": weights[0].item(), "gap_weight": weights[1].item(), "bias": bias}
    (output / "coefficients.json").write_text(json.dumps(coefficients, indent=2) + "\n")
    torch.save({"state_dict": model.state_dict(), "mean": mean, "std": std}, output / "calibration.pt")
    test = frame[frame.split == "test"].copy()
    inputs = torch.tensor(test[["blink", "gap"]].to_numpy(), dtype=torch.float32)
    with torch.inference_mode():
        probabilities = (inputs @ weights + bias).sigmoid().tolist()
    test["probability_closed"] = probabilities
    test["predicted_closed"] = test.probability_closed >= 0.5
    test["actual_closed"] = test.label == "closed"
    views = [{"view": view, **classification_metrics(group)} for view, group in test.groupby("view")]
    accepted = coefficients["blink_weight"] > 0 and coefficients["gap_weight"] < 0
    for view in ("left", "right"):
        row = next(row for row in views if row["view"] == view)
        accepted = accepted and row["tp"] + row["fn"] >= 5 and row["tn"] + row["fp"] >= 20
        accepted = accepted and (row["recall_closed"] or 0) >= 0.8 and row["false_positive_rate"] is not None and row["false_positive_rate"] <= 0.15
    report = {
        "counts": frame.groupby("split").size().to_dict(),
        "epochs": 400, "selection": "fixed final epoch", "seed": 42, "threshold": 0.5,
        "coefficients": coefficients, "train_class_weight": positive_weight.item(),
        "metrics": classification_metrics(test), "metrics_by_view": views,
        "accepted_for_integration": bool(accepted),
        "fingerprint": split["fingerprint"],
        "training_code_sha256": file_sha256(Path(__file__)),
        "manifest_sha256": file_sha256(manifest),
        "checkpoint_sha256": file_sha256(output / "calibration.pt"),
        "limitations": "Fixed pretrained features. Automatic yaw estimates only. No on-device 45-degree acceptance claim.",
    }
    (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    test.to_csv(output / "predictions.csv", index=False)
    print(json.dumps(report, indent=2), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--prior-manifest", type=Path, action="append", required=True)
    args = parser.parse_args()
    calibrate(args.root, args.output, args.prior_manifest)
