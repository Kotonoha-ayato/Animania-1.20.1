"""Validate a real client model/texture regression manifest.

This tool deliberately refuses to infer a visual pass from Java class names,
non-empty ModelParts, or a resource directory listing.  A graphics-capable
client capture process must provide one record per converted model entry with
an actual screenshot and a geometry/pose digest.  The resulting report is
the only shape accepted by the ``model_visual_regression`` release gate.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import sha256, write_json


STATES = {"idle", "walk", "run", "sleep", "eat", "drink", "play", "breed", "graze", "carry", "passenger", "towed"}
GENDERS = {"male", "female", "genderless", "unknown"}
AGES = {"baby", "adult"}


def safe(root: Path, value: str) -> Path | None:
    path = Path(value)
    if not value or path.is_absolute() or ".." in path.parts:
        return None
    absolute = root / path
    return absolute if absolute.is_file() else None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True,
                        help="capture manifest produced by the graphics-capable client harness")
    parser.add_argument("--output", type=Path, default=Path("build/model-visual-regression.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    manifest_path = args.manifest if args.manifest.is_absolute() else root / args.manifest
    output = args.output if args.output.is_absolute() else root / args.output
    errors: list[str] = []
    captures: list[dict] = []
    expected: set[str] = set()
    static = root / "docs/model-conversion-audit.json"
    if not static.is_file():
        errors.append("missing docs/model-conversion-audit.json")
    else:
        try:
            static_data = json.loads(static.read_text(encoding="utf-8"))
            expected = {str(entry.get("key")) for entry in static_data.get("entries", [])}
            if static_data.get("total") != 130 or static_data.get("error_count") != 0:
                errors.append("model conversion inventory is not the zero-error 130-entry baseline")
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"unreadable model conversion inventory: {exc}")
    if not manifest_path.is_file():
        errors.append(f"capture manifest missing: {manifest_path}")
        data = {}
    else:
        try:
            data = json.loads(manifest_path.read_text(encoding="utf-8"))
            captures = data.get("captures", [])
            if not isinstance(captures, list):
                captures = []
                errors.append("capture manifest captures must be an array")
        except (OSError, json.JSONDecodeError) as exc:
            data = {}
            errors.append(f"unreadable capture manifest: {exc}")
    seen_models: set[str] = set()
    for index, capture in enumerate(captures):
        label = f"capture[{index}]"
        if not isinstance(capture, dict):
            errors.append(f"{label}: record must be an object")
            continue
        model_key = capture.get("model_key")
        state = capture.get("state")
        gender = capture.get("gender")
        age = capture.get("age")
        if model_key not in expected:
            errors.append(f"{label}: unknown or missing model_key {model_key!r}")
        else:
            seen_models.add(model_key)
        if state not in STATES:
            errors.append(f"{label}: unsupported state {state!r}")
        if gender not in GENDERS:
            errors.append(f"{label}: unsupported gender {gender!r}")
        if age not in AGES:
            errors.append(f"{label}: unsupported age {age!r}")
        screenshot = safe(root, str(capture.get("screenshot", "")))
        if screenshot is None:
            errors.append(f"{label}: screenshot is missing or outside the repository")
        elif capture.get("screenshot_sha256") != sha256(screenshot):
            errors.append(f"{label}: screenshot SHA-256 does not match")
        geometry_digest = capture.get("geometry_sha256")
        if not isinstance(geometry_digest, str) or len(geometry_digest) != 64:
            errors.append(f"{label}: geometry_sha256 must be a 64-character digest from the capture harness")
        if capture.get("texture_resolved") is not True:
            errors.append(f"{label}: texture_resolved is not true")
        if not capture.get("selector"):
            errors.append(f"{label}: capture selector is missing")
    missing_models = sorted(expected - seen_models)
    if missing_models:
        errors.append(f"missing visual capture for {len(missing_models)} model entries")
    if not captures:
        errors.append("no client screenshots were executed")
    report = {
        "schema_version": 1,
        "audit": "model-visual-regression",
        "audit_version": "v1",
        "auditor_path": "tools/audit_model_visual_regression.py",
        "auditor_sha256": sha256(Path(__file__).resolve()),
        "manifest": str(manifest_path.relative_to(root)).replace("\\", "/") if manifest_path.is_relative_to(root) else str(manifest_path),
        "manifest_sha256": sha256(manifest_path) if manifest_path.is_file() else None,
        "expected_models": len(expected),
        "captured_models": len(seen_models),
        "capture_count": len(captures),
        "errors": errors,
        "error_count": len(errors),
        "evidence": [{"path": str(safe_path.relative_to(root)).replace("\\", "/"), "sha256": sha256(safe_path)}
                     for capture in captures
                     if isinstance(capture, dict)
                     for safe_path in [safe(root, str(capture.get("screenshot", "")))]
                     if safe_path is not None],
        "all_passed": not errors,
    }
    write_json(output, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
