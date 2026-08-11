"""Audit shipping model JSONs for placeholder and missing namespaced textures."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

MODULES = {
    "base": "animania",
    "farm": "animania_farm",
    "extra": "animania_extra",
    "catsdogs": "animania_catsdogs",
}
BAD_MARKERS = ("missingno", "placeholder", "minecraft:block/stone")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []
    model_count = 0
    texture_refs = 0
    for module, namespace in MODULES.items():
        model_dir = root / module / "src/main/resources/assets" / namespace / "models"
        texture_dir = root / module / "src/main/resources/assets" / namespace / "textures"
        if not model_dir.is_dir():
            errors.append(f"model directory missing: {model_dir}")
            continue
        for model in sorted(model_dir.rglob("*.json")):
            model_count += 1
            try:
                payload = json.loads(model.read_text(encoding="utf-8"))
            except Exception as exc:  # pragma: no cover - diagnostic path
                errors.append(f"invalid JSON {model}: {exc}")
                continue
            text = model.read_text(encoding="utf-8").lower()
            for marker in BAD_MARKERS:
                if marker in text:
                    errors.append(f"placeholder marker {marker!r} in {model}")
            for key, ref in payload.get("textures", {}).items():
                if not isinstance(ref, str) or ref.startswith("#"):
                    continue
                if ref.startswith("minecraft:"):
                    continue
                if ":" in ref and not ref.startswith(namespace + ":"):
                    continue
                path = ref.split(":", 1)[-1]
                if path.startswith("textures/"):
                    path = path[len("textures/"):]
                texture_counted = path + ".png"
                texture_refs += 1
                if not (texture_dir / texture_counted).is_file():
                    errors.append(f"missing texture {ref!r} ({key}) referenced by {model}")
    result = {"models": model_count, "namespaced_texture_refs": texture_refs,
              "error_count": len(errors), "errors": errors}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
