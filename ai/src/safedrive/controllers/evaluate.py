"""Batched held-out evaluation tied to the exact training dataset and checkpoint."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path

from safedrive.models.dataset import read_prepared_manifest
from safedrive.models.provenance import file_sha256, manifest_fingerprint


def classification_metrics(group):
    tp = int((group.predicted_closed & group.actual_closed).sum())
    fn = int((~group.predicted_closed & group.actual_closed).sum())
    fp = int((group.predicted_closed & ~group.actual_closed).sum())
    tn = int((~group.predicted_closed & ~group.actual_closed).sum())
    recall = tp / (tp + fn) if tp + fn else None
    specificity = tn / (tn + fp) if tn + fp else None
    return {"samples": len(group), "tp": tp, "fn": fn, "fp": fp, "tn": tn,
            "accuracy": (tp + tn) / len(group) if len(group) else None,
            "recall_closed": recall, "false_positive_rate": fp / (fp + tn) if fp + tn else None,
            "precision_closed": tp / (tp + fp) if tp + fp else None,
            "balanced_accuracy": (recall + specificity) / 2 if recall is not None and specificity is not None else None}


def verify_artifacts(frame, weights: Path):
    card_path = weights.parent / "model-card.json"
    if not card_path.is_file():
        raise ValueError("Missing model-card.json; use a tracked training checkpoint")
    card = json.loads(card_path.read_text())
    if not card.get("status", "").startswith("training complete"):
        raise ValueError("Finish the planned training before held-out evaluation")
    if card.get("dataset_fingerprint") != manifest_fingerprint(frame):
        raise ValueError("Evaluation manifest does not match the training dataset")
    if card.get("checkpoint_sha256") != file_sha256(weights):
        raise ValueError("Checkpoint does not match its model card")
    return card


def evaluate(manifest: Path, weights: Path, output: Path, threshold: float = 0.5, *,
             batch_size: int = 128, threads: int = 4):
    import cv2
    import pandas as pd
    import torch
    from torch.utils.data import DataLoader
    from safedrive.models.eye_model import EyeClassifier, EyeDataset

    if not 0 < threshold < 1:
        raise ValueError("threshold must be between zero and one")
    if batch_size < 1 or threads < 1:
        raise ValueError("batch_size and threads must be positive")
    if output.exists() and any(output.iterdir()):
        raise ValueError(f"Output is not empty: {output}")
    frame = read_prepared_manifest(manifest)
    card = verify_artifacts(frame, weights)
    test = frame[frame.split == "test"].copy()
    torch.set_num_threads(threads)
    cv2.setNumThreads(1)
    model = EyeClassifier(pool_size=card.get("architecture", {}).get("pool_size", 1))
    model.load_state_dict(torch.load(weights, map_location="cpu", weights_only=True))
    model.eval()
    probabilities = []
    loader = DataLoader(EyeDataset(test), batch_size=batch_size, shuffle=False)
    with torch.inference_mode():
        for images, _ in loader:
            probabilities.extend(model(images).sigmoid().tolist())
    test["probability_closed"] = probabilities
    test["predicted_closed"] = test.probability_closed >= threshold
    test["actual_closed"] = test.label == "closed"
    summary = {
        "evaluated_at_utc": datetime.now(timezone.utc).isoformat(),
        "split": "test", "subjects": int(test.subject_id.nunique()), "threshold": threshold,
        "checkpoint_sha256": file_sha256(weights), "dataset_fingerprint": card["dataset_fingerprint"],
        "best_epoch": card["best_epoch"], "torch_version": str(torch.__version__),
        "device": "cpu", "batch_size": batch_size, "metrics": classification_metrics(test),
        "limitations": "Eye-crop frame classification only; no event-level, multiview, RGB phone or alarm validation",
    }
    conditions = []
    for column in ("view", "glasses", "lighting", "reflections", "sensor", "subject_id"):
        if column in test:
            for value, group in test.groupby(column, sort=True):
                conditions.append({"condition": column, "value": str(value), **classification_metrics(group)})
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    condition_frame = pd.DataFrame(conditions)
    condition_frame.to_csv(output / "metrics_by_condition.csv", index=False)
    condition_frame[condition_frame.condition == "view"].rename(columns={"value": "view"}).drop(
        columns="condition").to_csv(output / "metrics_by_view.csv", index=False)
    test.to_csv(output / "predictions.csv", index=False)
    print(json.dumps(summary, indent=2), flush=True)
    return summary


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("weights", type=Path)
    parser.add_argument("--output", type=Path, default=Path("artifacts/evaluation"))
    parser.add_argument("--threshold", type=float, default=0.5)
    parser.add_argument("--batch-size", type=int, default=128)
    parser.add_argument("--threads", type=int, default=4)
    args = parser.parse_args()
    evaluate(args.manifest, args.weights, args.output, args.threshold,
             batch_size=args.batch_size, threads=args.threads)


if __name__ == "__main__":
    main()
