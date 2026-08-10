"""Convert the pinned 1.12 ModelRenderer boxes into native 1.20.1 LayerDefinitions."""
from __future__ import annotations

import argparse
import ast
import math
import re
from dataclasses import dataclass, field
from pathlib import Path

MOD_IDS = {"farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}


def snake(value: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", value).lower()


def number(expr: str) -> float:
    value = re.sub(r"(?<=\d)[fFdD]\b", "", expr.strip())
    value = value.replace("(float)", "").replace("Math.PI", str(math.pi)).strip()
    # ModelChick contains the historical literal -073803F; Java intended
    # -0.73803F (and accepted neither form on modern compilers).
    value = re.sub(r"^([+-]?)0+(\d{2,})$", lambda m: m.group(1) + "0." + m.group(2), value)
    try:
        tree = ast.parse(value, mode="eval")
    except (SyntaxError, IndentationError) as exc:
        raise ValueError(expr) from exc
    allowed = (ast.Expression, ast.Constant, ast.UnaryOp, ast.BinOp, ast.Add, ast.Sub,
               ast.Mult, ast.Div, ast.USub, ast.UAdd)
    if not all(isinstance(node, allowed) for node in ast.walk(tree)):
        raise ValueError(expr)
    return float(eval(compile(tree, "<model-number>", "eval"), {"__builtins__": {}}))


def args(text: str) -> list[float]:
    return [number(value) for value in text.split(",")]


def f(value: float) -> str:
    if abs(value) < 0.0000001:
        value = 0
    literal = f"{value:.6f}".rstrip("0").rstrip(".")
    if "." not in literal:
        literal += ".0"
    return literal + "F"


@dataclass
class Part:
    name: str
    uv: tuple[int, int] = (0, 0)
    boxes: list[tuple[float, ...]] = field(default_factory=list)
    pos: tuple[float, float, float] = (0, 0, 0)
    rot: tuple[float, float, float] = (0, 0, 0)
    children: list[str] = field(default_factory=list)
    mirror: bool = False


@dataclass
class Model:
    name: str
    width: int
    height: int
    parts: dict[str, Part]


def parse_model(path: Path) -> Model:
    text = re.sub(r"//.*", "", path.read_text(encoding="utf-8", errors="replace"))
    parts: dict[str, Part] = {}
    for match in re.finditer(r"(?:this\.)?(\w+)\s*=\s*new\s+ModelRenderer(?:Animania)?\s*\(\s*(?:this\s*,\s*)?([^)]*)\)", text):
        name, raw = match.groups()
        values = args(raw) if raw.strip() else []
        uv = (int(values[-2]), int(values[-1])) if len(values) >= 2 else (0, 0)
        parts[name] = Part(name, uv)
    width = height = 64
    for match in re.finditer(r"(?:this\.)?(\w+)\.setTextureSize\s*\(([^)]*)\)", text):
        try:
            values = args(match.group(2)); width = max(width, int(values[0])); height = max(height, int(values[1]))
        except (ValueError, IndexError):
            pass
    for match in re.finditer(r"(?:this\.)?(\w+)\.addBox\s*\(([^)]*)\)", text):
        name = match.group(1)
        if name not in parts: continue
        try:
            values = tuple(args(match.group(2)))
            if len(values) >= 6: parts[name].boxes.append(values[:7])
        except ValueError:
            pass
    for match in re.finditer(r"(?:this\.)?(\w+)\.setRotationPoint\s*\(([^)]*)\)", text):
        if match.group(1) in parts:
            try: parts[match.group(1)].pos = tuple(args(match.group(2))[:3])
            except ValueError: pass
    for match in re.finditer(r"(?:this\.)?(\w+)\.setOffset\s*\(([^)]*)\)", text):
        if match.group(1) in parts:
            try:
                offset = args(match.group(2))[:3]
                old = parts[match.group(1)].pos
                parts[match.group(1)].pos = tuple(old[i] + offset[i] for i in range(3))
            except ValueError: pass
    rotations: dict[str, list[float]] = {name: [0, 0, 0] for name in parts}
    axes = {"X": 0, "Y": 1, "Z": 2}
    for match in re.finditer(r"(?:this\.)?(\w+)\.rotateAngle([XYZ])\s*=\s*([^;]+);", text):
        name, axis, raw = match.groups()
        if name in rotations:
            try: rotations[name][axes[axis]] = number(raw)
            except ValueError: pass
    for match in re.finditer(r"setRotateAngle\s*\(\s*(?:this\.)?(\w+)\s*,([^)]*)\)", text):
        if match.group(1) in rotations:
            try: rotations[match.group(1)] = args(match.group(2))[:3]
            except ValueError: pass
    for name, rotation in rotations.items(): parts[name].rot = tuple(rotation)
    for match in re.finditer(r"(?:this\.)?(\w+)\.addChild\s*\(\s*(?:this\.)?(\w+)\s*\)", text):
        parent, child = match.groups()
        if parent in parts and child in parts and child not in parts[parent].children:
            parts[parent].children.append(child)
    for match in re.finditer(r"(?:this\.)?(\w+)\.mirror\s*=\s*true", text):
        if match.group(1) in parts: parts[match.group(1)].mirror = True
    if not parts:
        raise ValueError(f"no ModelRenderer geometry in {path}")
    return Model(path.stem, width, height, parts)


def find_model(source: Path, name: str) -> Path | None:
    matches = list(source.rglob(name + ".java"))
    return matches[0] if matches else None


def mappings(root: Path, module: str) -> dict[str, tuple[str, float]]:
    addon = root / "upstream/Animania-1.12/src/main/java/com/animania/addons" / module
    result: dict[str, tuple[str, float]] = {}
    if module in {"farm", "extra"}:
        for path in (addon / "client").rglob("Render*.java"):
            text = path.read_text(encoding="utf-8", errors="replace")
            renderer = re.search(r"class\s+Render(\w+)", text)
            model = re.search(r"new\s+(Model\w+)\s*\(", text)
            if renderer and model:
                scales = re.findall(r"glScalef\s*\(\s*([0-9.]+)F", text)
                result[snake(renderer.group(1))] = (model.group(1), float(scales[0]) if scales else 1.0)
        if module == "farm":
            for breed in ("leghorn", "orpington", "plymouth_rock", "rhode_island_red", "wyandotte"):
                for role, model in (("chick", "ModelChick"), ("hen", "ModelHen"), ("rooster", "ModelRooster")):
                    result[f"{role}_{breed}"] = (model, 1.0)
            for role, model in (("foal", "ModelDraftHorseFoal"), ("mare", "ModelDraftHorseMare"), ("stallion", "ModelDraftHorseStallion")):
                result[f"{role}_draft"] = (model, 1.0)
        else:
            result["frog"] = ("ModelFrog", 1.0); result["dartfrog"] = ("ModelFrog", 1.0)
            for color in ("blue", "charcoal", "opal", "peach", "purple", "taupe", "white"):
                result[f"peachick_{color}"] = ("ModelPeachick", 1.0)
                result[f"peacock_{color}"] = ("ModelPeacock", 1.0)
                result[f"peahen_{color}"] = ("ModelPeafowl", 1.0)
    else:
        handler = addon / "client/CatsDogsAddonRenderHandler.java"
        text = handler.read_text(encoding="utf-8", errors="replace")
        pattern = r"Entity(\w+)\.class\s*,\s*new\s+\w+\.Factory\s*\(\s*new\s+(Model\w+)\s*\(\)"
        for entity, model in re.findall(pattern, text): result[snake(entity)] = (model, 1.0)
        # Fox uses a dedicated renderer but the same Java model conversion path.
        for role in ("male", "female", "puppy"): result[f"{role}_fox"] = ("ModelFox", 1.0)
    return result


def emit_part(lines: list[str], model: Model, part: Part, variable: str, indent: str) -> None:
    builder = "CubeListBuilder.create()"
    if part.mirror: builder += ".mirror()"
    for box in part.boxes:
        builder += f".texOffs({part.uv[0]}, {part.uv[1]}).addBox({', '.join(f(v) for v in box[:6])}"
        if len(box) == 7: builder += f", new CubeDeformation({f(box[6])})"
        builder += ")"
    x, y, z = part.pos; rx, ry, rz = part.rot
    pose = f"PartPose.offsetAndRotation({f(x)}, {f(y)}, {f(z)}, {f(rx)}, {f(ry)}, {f(rz)})"
    safe = snake(part.name)
    lines.append(f'{indent}PartDefinition {safe} = {variable}.addOrReplaceChild("{safe}", {builder}, {pose});')
    for child in part.children:
        emit_part(lines, model, model.parts[child], safe, indent)


def emit(root: Path, module: str) -> None:
    mapping = mappings(root, module)
    ids_path = root / module / "src/main/java/com/animania" / module / (("CatsDogs" if module == "catsdogs" else module.title()) + "LegacyIds.java")
    text = ids_path.read_text(encoding="utf-8")
    ids = re.findall(r'"([a-z0-9_]+)"', text.split("List.of(", 1)[1].split(");", 1)[0])
    ids = [item for item in ids if item not in {"cart", "wagon", "tiller"}]
    missing = [item for item in ids if item not in mapping]
    if missing: raise SystemExit(f"{module}: missing renderer mappings: {missing}")
    model_source = root / "upstream/Animania-1.12/src/main/java/com/animania/addons" / module / "client"
    models: dict[str, Model] = {}
    for name in {mapping[entity][0] for entity in ids}:
        if name not in models:
            path = find_model(model_source, name)
            if path is None: raise SystemExit(f"{module}: missing source model {name}")
            models[name] = parse_model(path)
    class_name = ("CatsDogs" if module == "catsdogs" else module.title()) + "LegacyModelLayers"
    package = f"com.animania.{module}.client.model"
    lines = [f"package {package};", "", "// Generated from the pinned LGPL-3.0 Animania 1.12 Java models; do not edit by hand.",
             "import java.util.LinkedHashMap;", "import java.util.Map;",
             "import net.minecraft.client.model.geom.ModelLayerLocation;", "import net.minecraft.client.model.geom.PartPose;",
             "import net.minecraft.client.model.geom.builders.CubeDeformation;", "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
             "import net.minecraft.client.model.geom.builders.LayerDefinition;", "import net.minecraft.client.model.geom.builders.MeshDefinition;",
             "import net.minecraft.client.model.geom.builders.PartDefinition;", "import net.minecraft.resources.ResourceLocation;", "",
             f"public final class {class_name} {{", "    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();",
             "    static {"]
    for entity in ids:
        lines.append(f'        LAYERS.put("{entity}", new ModelLayerLocation(new ResourceLocation("{MOD_IDS[module]}", "{entity}"), "main"));')
    lines += ["    }", f"    private {class_name}() {{}}", "    public static LayerDefinition create(String id) {", "        return switch (id) {"]
    reverse: dict[str, list[str]] = {}
    for entity in ids: reverse.setdefault(mapping[entity][0], []).append(entity)
    for model_name, entity_ids in reverse.items():
        labels = ", ".join(f'"{entity}"' for entity in entity_ids)
        lines.append(f"            case {labels} -> {snake(model_name)}();")
    lines += ['            default -> throw new IllegalArgumentException("Unknown legacy model " + id);', "        };", "    }"]
    for name, model in models.items():
        method = snake(name)
        lines += [f"    private static LayerDefinition {method}() {{", "        MeshDefinition mesh = new MeshDefinition();", "        PartDefinition root = mesh.getRoot();"]
        children = {child for part in model.parts.values() for child in part.children}
        used: set[str] = set()
        for part in model.parts.values():
            if part.name not in children and part.name not in used:
                emit_part(lines, model, part, "root", "        ")
                used.add(part.name)
        lines += [f"        return LayerDefinition.create(mesh, {model.width}, {model.height});", "    }"]
    lines.append("}")
    output = root / module / "src/main/java/com/animania" / module / "client/model" / f"{class_name}.java"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(module, len(ids), len(models), output)


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    opts = parser.parse_args()
    for module in ("farm", "extra", "catsdogs"): emit(opts.root, module)


if __name__ == "__main__": main()
