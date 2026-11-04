"""Report held-out eye-state performance by camera view, without training on test."""

import argparse
from pathlib import Path


def evaluate(manifest: Path, weights: Path, output: Path, threshold: float = 0.5):
    import pandas as pd
    import torch
    from safedrive.models.eye_model import EyeClassifier, load_eye

    if not 0 < threshold < 1:
        raise ValueError("threshold must be between zero and one")
    frame = pd.read_csv(manifest, dtype=str, keep_default_na=False)
    if frame.groupby("subject_id").split.nunique().max() != 1:
        raise ValueError("Subject leakage across splits")
    test = frame[frame.split == "test"].copy()
    if test.empty:
        raise ValueError("No test examples")
    model = EyeClassifier()
    model.load_state_dict(torch.load(weights, map_location="cpu", weights_only=True))
    model.eval()
    with torch.inference_mode():
        test["probability_closed"] = [
            model(load_eye(path).unsqueeze(0)).sigmoid().item() for path in test.image_path
        ]
    test["predicted_closed"] = test.probability_closed >= threshold
    test["actual_closed"] = test.label == "closed"
    rows = []
    for view, group in test.groupby("view"):
        tp = int((group.predicted_closed & group.actual_closed).sum())
        fn = int((~group.predicted_closed & group.actual_closed).sum())
        fp = int((group.predicted_closed & ~group.actual_closed).sum())
        tn = int((~group.predicted_closed & ~group.actual_closed).sum())
        rows.append({"view": view, "samples": len(group), "tp": tp, "fn": fn,
                     "fp": fp, "tn": tn,
                     "recall_closed": tp / (tp + fn) if tp + fn else None,
                     "false_positive_rate": fp / (fp + tn) if fp + tn else None})
    output.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(rows).to_csv(output / "metrics_by_view.csv", index=False)
    test.to_csv(output / "predictions.csv", index=False)
    print(pd.DataFrame(rows).to_string(index=False))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("weights", type=Path)
    parser.add_argument("--output", type=Path, default=Path("artifacts/evaluation"))
    parser.add_argument("--threshold", type=float, default=0.5)
    args = parser.parse_args()
    evaluate(args.manifest, args.weights, args.output, args.threshold)


if __name__ == "__main__":
    main()
