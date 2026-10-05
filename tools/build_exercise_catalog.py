#!/usr/bin/env python3
"""Builds app/src/main/assets/exercise_catalog.json from free-exercise-db (Unlicense).

Usage: tools/build_exercise_catalog.py [exercises.json]
Without an argument the combined file is downloaded from GitHub.

Kept: strength, powerlifting, olympic weightlifting and strongman exercises
(stretching, cardio and plyometrics are left out). Muscles and equipment are
mapped to the app's enums; entries without a mappable primary muscle or without instructions are dropped.
Images are built separately by tools/build_exercise_images.py.
"""
import json
import sys
import urllib.request

URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
OUT = "app/src/main/assets/exercise_catalog.json"

CATEGORIES = {"strength", "powerlifting", "olympic weightlifting", "strongman"}

EQUIPMENT = {
    "barbell": "BARBELL",
    "e-z curl bar": "BARBELL",
    "dumbbell": "DUMBBELL",
    "cable": "CABLE",
    "machine": "MACHINE",
    "kettlebells": "KETTLEBELL",
    "bands": "BAND",
    "body only": "BODYWEIGHT",
    "medicine ball": "OTHER",
    "exercise ball": "OTHER",
    "other": "OTHER",
    None: "OTHER",
}

MUSCLES = {
    "abdominals": "ABS",
    "abductors": "GLUTES",
    "adductors": "ADDUCTORS",
    "biceps": "BICEPS",
    "calves": "CALVES",
    "chest": "CHEST",
    "forearms": "FOREARMS",
    "glutes": "GLUTES",
    "hamstrings": "HAMSTRINGS",
    "lats": "LATS",
    "lower back": "LOWER_BACK",
    "middle back": "UPPER_BACK",
    "quadriceps": "QUADS",
    "shoulders": "SHOULDERS",
    "traps": "UPPER_BACK",
    "triceps": "TRICEPS",
}


def muscles(names):
    result = []
    for name in names:
        mapped = MUSCLES.get(name)
        if mapped and mapped not in result:
            result.append(mapped)
    return result


def reps(item, equipment):
    category = item["category"]
    if category == "olympic weightlifting":
        return [2, 5]
    if category == "powerlifting":
        return [3, 6]
    if category == "strongman":
        return [4, 8]
    if equipment == "BODYWEIGHT":
        return [8, 15]
    if item.get("mechanic") == "isolation":
        return [10, 15]
    return [6, 10] if equipment == "BARBELL" else [8, 12]


def main():
    if len(sys.argv) > 1:
        data = json.load(open(sys.argv[1], encoding="utf-8"))
    else:
        data = json.load(urllib.request.urlopen(URL))

    entries = []
    for item in data:
        if item["category"] not in CATEGORIES or item.get("equipment") == "foam roll":
            continue
        primary = muscles(item["primaryMuscles"])
        instructions = [step.strip() for step in item.get("instructions", []) if step.strip()]
        if not primary or not instructions:
            continue
        secondary = [m for m in muscles(item["secondaryMuscles"]) if m not in primary]
        equipment = EQUIPMENT[item.get("equipment")]
        min_reps, max_reps = reps(item, equipment)
        entries.append({
            "id": item["id"],
            "name": item["name"].strip(),
            "type": "BODYWEIGHT" if equipment == "BODYWEIGHT" else "STRENGTH",
            "equipment": equipment,
            "primary": primary,
            "secondary": secondary,
            "repMin": min_reps,
            "repMax": max_reps,
            "instructions": instructions,
        })
    entries.sort(key=lambda e: e["name"].lower())
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(entries, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    print(f"{len(entries)} exercises -> {OUT}")


if __name__ == "__main__":
    main()
