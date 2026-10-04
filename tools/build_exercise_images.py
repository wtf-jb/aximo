#!/usr/bin/env python3
"""Builds app/src/main/assets/exercise_images/ from free-exercise-db (Unlicense).

Usage: tools/build_exercise_images.py <path to a free-exercise-db checkout>
  git clone --depth 1 https://github.com/yuhonas/free-exercise-db.git

For every entry of app/src/main/assets/exercise_catalog.json the two photos
(start and end position) are scaled to WIDTH px and saved as WebP:
exercise_images/<id>/0.webp and 1.webp. Entries without photos get no folder.
Needs Pillow (pip install pillow).
"""
import json
import os
import shutil
import sys

from PIL import Image

CATALOG = "app/src/main/assets/exercise_catalog.json"
OUT = "app/src/main/assets/exercise_images"
WIDTH = 400
QUALITY = 70


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    source = os.path.join(sys.argv[1], "exercises")
    with open(CATALOG, encoding="utf-8") as f:
        entries = json.load(f)

    shutil.rmtree(OUT, ignore_errors=True)
    written = 0
    for entry in entries:
        for index in (0, 1):
            path = os.path.join(source, entry["id"], f"{index}.jpg")
            if not os.path.exists(path):
                continue
            image = Image.open(path).convert("RGB")
            height = round(image.height * WIDTH / image.width)
            target = os.path.join(OUT, entry["id"], f"{index}.webp")
            os.makedirs(os.path.dirname(target), exist_ok=True)
            image.resize((WIDTH, height), Image.LANCZOS).save(target, "WEBP", quality=QUALITY, method=6)
            written += 1
    print(f"{written} images for {len(entries)} entries written to {OUT}")


if __name__ == "__main__":
    main()
