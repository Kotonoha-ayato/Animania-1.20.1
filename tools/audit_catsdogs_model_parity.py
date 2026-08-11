"""Fail-closed, per-model Cats & Dogs parity audit against pinned 1.12 source."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

import convert_legacy_java_models as converter


def method_body(text: str, name: str) -> str:
    match = re.search(rf"private static LayerDefinition\s+{re.escape(name)}\s*\(\)\s*\{{", text)
    if match is None:
        return ""
    start = text.find("{", match.start())
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1:index]
    return ""


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def audit(root: Path) -> dict[str, object]:
    source_root = root / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models"
    generated_path = root / "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsLegacyModelLayers.java"
    generated = generated_path.read_text(encoding="utf-8")
    source_paths = sorted([*source_root.glob("cats/Model*.java"), *source_root.glob("dogs/Model*.java")])
    model_results: list[dict[str, object]] = []
    by_name: dict[str, bool] = {}
    for source in source_paths:
        model = converter.parse_model(source)
        pose = converter.sleeping_pose(root, source)
        body = method_body(generated, converter.snake(model.name))
        expected_lines = ["MeshDefinition mesh = new MeshDefinition();", "PartDefinition root = mesh.getRoot();"]
        children = {child for part in model.parts.values() for child in part.children}
        for part in model.parts.values():
            if part.name not in children:
                converter.emit_part(expected_lines, model, part, "root", "")
        expected_lines.append(f"return LayerDefinition.create(mesh, {model.width}, {model.height});")
        normalize = lambda value: re.sub(r"\s+", "", value)
        source_gait = len(set(re.findall(
            r"(?:this\.)?(\w+)\.rotateAngleX\s*=\s*MathHelper\.cos\s*\(",
            source.read_text(encoding="utf-8", errors="replace"),
        )))
        profile = converter.animation_profile(model, source)
        generated_gait = len(profile[1]) + len(profile[2])
        common_sleep = set(model.parts) & set(pose.parts)
        checks = {
            "geometry_exact": normalize(body) == normalize("\n".join(expected_lines)),
            "part_nodes": body.count("addOrReplaceChild(") == len(model.parts) * 2,
            "cube_count": body.count(".addBox(") == sum(len(part.boxes) for part in model.parts.values()),
            "child_edges": sum(len(part.children) for part in model.parts.values()) == len(model.parts) - 1,
            "gait_assignments": source_gait == generated_gait and source_gait == 4,
            "sitting_pose": converter.sitting_pose_java(model) in generated,
            "sleeping_pose": (len(common_sleep) > 10
                              and converter.pet_animation_java(model, source, pose) in generated),
        }
        passed = all(checks.values())
        by_name[model.name] = passed
        model_results.append({
            "model": model.name,
            "family": source.parent.name,
            "source": source.relative_to(root).as_posix(),
            "source_sha256": sha(source),
            "parts": len(model.parts),
            "cubes": sum(len(part.boxes) for part in model.parts.values()),
            "child_edges": sum(len(part.children) for part in model.parts.values()),
            "sitting_parts": len(model.sitting_pose),
            "sleeping_pose": pose.name,
            "sleeping_common_parts": len(common_sleep),
            "gait_parts": generated_gait,
            "checks": checks,
            "status": "pass" if passed else "fail",
        })

    # ModelPetBowl is the 23rd legacy Java model.  It is deliberately reported
    # separately: the active 1.12 TESR renders a 13-part shell plus 71 colored
    # kibble cubes, whereas the current native layer contains only the shell.
    # A non-empty bake must not be allowed to close that missing behavior.
    bowl_source = source_root / "blocks/ModelPetBowl.java"
    bowl_text = bowl_source.read_text(encoding="utf-8", errors="replace")
    native_path = root / "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsNativeModelLayers.java"
    native_body = method_body(native_path.read_text(encoding="utf-8"), "model_pet_bowl")
    bowl_parts = len(re.findall(r"\bModelRenderer(?:Colored)?\s+\w+\s*;", bowl_text))
    bowl_colored = len(re.findall(r"\bModelRendererColored\s+\w+\s*;", bowl_text))
    bowl_checks = {
        "shell_present": native_body.count("addOrReplaceChild(") >= 13,
        "all_parts_converted": native_body.count("addOrReplaceChild(") >= bowl_parts,
        "colored_food_rendered": "renderFood" in (root / "catsdogs/src/main/java/com/animania/catsdogs/client/render/CatsDogsPetBowlRenderer.java").read_text(encoding="utf-8"),
    }
    bowl_passed = all(bowl_checks.values())
    model_results.append({
        "model": "ModelPetBowl",
        "family": "blocks",
        "source": bowl_source.relative_to(root).as_posix(),
        "source_sha256": sha(bowl_source),
        "parts": bowl_parts,
        "colored_food_parts": bowl_colored,
        "checks": bowl_checks,
        "status": "pass" if bowl_passed else "requires_implementation",
    })

    mappings = converter.mappings(root, "catsdogs")
    translations = converter.catsdogs_translations(root)
    entity_results = []
    for entity_id in sorted(mappings):
        model_name, scale = mappings[entity_id]
        checks = {
            "model": by_name.get(model_name, False),
            "scale": isinstance(scale, float) and scale > 0.0,
            "translation": entity_id in translations,
        }
        entity_results.append({
            "id": f"animania_catsdogs:{entity_id}",
            "model": model_name,
            "scale": scale,
            "translation": translations.get(entity_id),
            "checks": checks,
            "status": "pass" if all(checks.values()) else "fail",
        })
    failures = sum(item["status"] != "pass" for item in model_results + entity_results)
    return {
        "audit": "catsdogs-model-parity",
        "version": 1,
        "baseline": "Animania 1.12 pinned source",
        "generated_sha256": sha(generated_path),
        "model_count": len(model_results),
        "entity_count": len(entity_results),
        "failures": failures,
        "status": "pass" if failures == 0 and len(model_results) == 23 and len(entity_results) == 69 else "fail",
        "models": model_results,
        "entities": entity_results,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.root.resolve())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ("status", "model_count", "entity_count", "failures")}, ensure_ascii=False))
    raise SystemExit(0 if report["status"] == "pass" else 1)


if __name__ == "__main__":
    main()
