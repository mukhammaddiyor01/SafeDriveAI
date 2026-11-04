import argparse
from pathlib import Path
from safedrive.models.dataset import prepare_manifest


def main():
    parser = argparse.ArgumentParser(description="Prepare labeled eye-crop dataset")
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--output", type=Path, default=Path("artifacts/manifest.csv"))
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    print(prepare_manifest(args.manifest, args.output, args.seed))


if __name__ == "__main__":
    main()
