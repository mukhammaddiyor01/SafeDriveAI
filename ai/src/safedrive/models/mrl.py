"""MRL's filename labels, in the order documented by the dataset authors."""

from dataclasses import dataclass
from pathlib import Path
import re

SOURCE_URL = "https://mrl.cs.vsb.cz/eyedataset.html"
ARCHIVE_URL = "https://mrl.cs.vsb.cz/data/eyedataset/mrlEyes_2018_01.zip"


@dataclass(frozen=True)
class MrlLabels:
    subject_id: str
    image_id: str
    gender: str
    glasses: str
    label: str
    reflections: str
    lighting: str
    sensor: str


def parse_filename(path: Path) -> MrlLabels:
    parts = path.stem.split("_")
    if path.suffix.lower() != ".png" or len(parts) != 8:
        raise ValueError(f"Invalid MRL image filename: {path.name}")
    subject, image, gender, glasses, state, reflections, lighting, sensor = parts
    if not re.fullmatch(r"s\d{4}", subject) or not image.isdigit():
        raise ValueError(f"Invalid MRL subject/image ID: {path.name}")
    for value, allowed in ((gender, {"0", "1"}), (glasses, {"0", "1"}),
                           (state, {"0", "1"}), (reflections, {"0", "1", "2"}),
                           (lighting, {"0", "1"}), (sensor, {"01", "02", "03"})):
        if value not in allowed:
            raise ValueError(f"Invalid MRL label: {path.name}")
    return MrlLabels(
        subject_id=f"mrl_{subject}", image_id=image, gender=gender, glasses=glasses,
        label="closed" if state == "0" else "open", reflections=reflections,
        lighting=lighting, sensor=sensor,
    )
