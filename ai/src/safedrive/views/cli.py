import argparse
from pathlib import Path
from safedrive.models.dataset import prepare_manifest


def main():
    parser = argparse.ArgumentParser(description="Prepare labeled eye-crop dataset")
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--output", type=Path, default=Path("artifacts/manifest.csv"))
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--prior-manifest", type=Path, action="append", default=[],
                        help="Previously used dataset manifest; repeat for each experiment history")
    args = parser.parse_args()
    print(prepare_manifest(args.manifest, args.output, args.seed, prior_manifests=args.prior_manifest))


if __name__ == "__main__":
    main()
