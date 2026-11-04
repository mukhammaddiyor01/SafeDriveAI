"""YOLO head/shoulder experiment. This is not an eye-closure detector."""

import argparse
from pathlib import Path


def run(source: str, weights: str, output: Path):
    import cv2
    import pandas as pd
    from ultralytics import YOLO

    model = YOLO(weights)
    capture = cv2.VideoCapture(int(source) if source.isdigit() else source)
    if not capture.isOpened():
        raise RuntimeError(f"Cannot open video source: {source}")
    rows = []
    columns = ["frame", "person", "keypoint", "x", "y", "confidence"]
    output.parent.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(columns=columns).to_csv(output, index=False)

    def flush():
        if rows:
            pd.DataFrame(rows, columns=columns).to_csv(output, mode="a", header=False, index=False)
            rows.clear()

    frame_number = 0
    try:
        while True:
            ok, frame = capture.read()
            if not ok:
                break
            result = model.predict(frame, imgsz=320, conf=0.4, verbose=False)[0]
            if result.keypoints is not None and result.keypoints.conf is not None:
                for person_id, (points, confidences) in enumerate(zip(
                    result.keypoints.xy.cpu().numpy(), result.keypoints.conf.cpu().numpy()
                )):
                    # COCO has no neck point. Never claim the shoulder midpoint is a measured neck.
                    for name, index in (("nose", 0), ("left_eye", 1), ("right_eye", 2),
                                        ("left_ear", 3), ("right_ear", 4),
                                        ("left_shoulder", 5), ("right_shoulder", 6)):
                        x, y = points[index]
                        confidence = float(confidences[index])
                        rows.append({"frame": frame_number, "person": person_id,
                                     "keypoint": name, "x": float(x), "y": float(y),
                                     "confidence": confidence})
                        if confidence >= 0.4:
                            cv2.circle(frame, (int(x), int(y)), 4, (40, 200, 255), -1)
            cv2.imshow("SafeDriveAI pose research — Q to close", frame)
            frame_number += 1
            if frame_number % 100 == 0:
                flush()
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    finally:
        capture.release()
        cv2.destroyAllWindows()
        flush()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default="0")
    parser.add_argument("--weights", default="yolo11n-pose.pt")
    parser.add_argument("--output", type=Path, default=Path("artifacts/pose.csv"))
    args = parser.parse_args()
    run(args.source, args.weights, args.output)


if __name__ == "__main__":
    main()
