"""Small file and prepared-manifest identities for traceable model artifacts."""

import hashlib
from pathlib import Path


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def manifest_fingerprint(frame) -> str:
    # Image hashes permit relocation without changing the dataset identity.
    columns = sorted(set(frame.columns) - ({"image_path"} if "image_sha256" in frame else set()))
    canonical = frame[columns].sort_values(columns).to_csv(index=False, lineterminator="\n")
    return hashlib.sha256(canonical.encode()).hexdigest()
