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


def expand_indexed_part_loops(text: str) -> str:
    """Expand simple constructor loops used to create ModelRenderer arrays (hamster cheeks)."""
    lengths = {name: int(size) for name, size in re.findall(
        r"(?:this\.)?(\w+)\s*=\s*new\s+ModelRenderer\s*\[\s*(\d+)\s*\]", text)}
    pattern = re.compile(
        r"for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;\s*\1\s*<\s*(?:this\.)?(\w+)\.length\s*;\s*\1\+\+\s*\)\s*\{(.*?)\}",
        re.S)

    def expand(match: re.Match[str]) -> str:
        index, array, body = match.groups()
        if array not in lengths:
            return match.group(0)
        copies = []
        for value in range(lengths[array]):
            copy = re.sub(rf"(?:this\.)?{re.escape(array)}\s*\[\s*{re.escape(index)}\s*\]",
                          f"this.{array}{value}", body)
            copy = re.sub(rf"\b{re.escape(index)}\b", str(value), copy)
            copies.append(copy)
        return "\n".join(copies)

    return pattern.sub(expand, text)


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
    # ModelRendererAnimania applies this translation after its rotation.  It
    # is deliberately distinct from ``pos`` (the pre-rotation pivot).
    offset: tuple[float, float, float] = (0, 0, 0)
    rot: tuple[float, float, float] = (0, 0, 0)
    children: list[str] = field(default_factory=list)
    mirror: bool = False
    gait_phase: int | None = None
    colored: bool = False


@dataclass
class PoseOverride:
    pos: tuple[float, float, float] | None = None
    rot: tuple[float | None, float | None, float | None] = (None, None, None)


@dataclass
class Model:
    name: str
    width: int
    height: int
    parts: dict[str, Part]
    private_parts: set[str]
    sitting_pose: dict[str, PoseOverride] = field(default_factory=dict)


def method_body(text: str, signature: re.Pattern[str]) -> str | None:
    """Return the body for a Java method/constructor matched through ``{``."""
    match = signature.search(text)
    if match is None:
        return None
    start = text.find("{", match.start(), match.end())
    if start < 0:
        return None
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1:index]
    return None


def parse_model(path: Path) -> Model:
    # Geometry in a commented-out prototype must never become live again in
    # the converted model (the Angus source, for example, documents unused
    # horns in a block comment). Strip both Java comment forms before any
    # ModelRenderer pattern is considered.
    text = path.read_text(encoding="utf-8", errors="replace")
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    text = expand_indexed_part_loops(text)
    constructor = method_body(
        text,
        re.compile(rf"\bpublic\s+{re.escape(path.stem)}\s*\(\s*\)\s*\{{"),
    )
    # Geometry and default pivots belong to the no-argument constructor.
    # Scanning the full file accidentally promoted literal values in
    # setLivingAnimations/setRotationAngles (notably dog sitting poses) into
    # the initial mesh pose.
    geometry = constructor if constructor is not None else text
    defaults = geometry
    if constructor is not None and re.search(r"(?:this\.)?setupAngles\s*\(\s*\)", constructor):
        setup = method_body(text, re.compile(r"\b(?:public|private|protected)?\s*(?:final\s+)?void\s+setupAngles\s*\(\s*\)\s*\{"))
        if setup is not None:
            defaults += "\n" + setup
    constants = {name: value for name, value in re.findall(
        r"\b(?:float|double)\s+(\w+)\s*=\s*([+-]?(?:\d+(?:\.\d*)?|\.\d+)[fFdD]?)\s*;", geometry)}

    def model_args(raw: str) -> list[float]:
        for name, value in constants.items():
            raw = re.sub(rf"\b{re.escape(name)}\b", value, raw)
        return args(raw)

    parts: dict[str, Part] = {}
    for match in re.finditer(r"(?:this\.)?(\w+)\s*=\s*new\s+(ModelRenderer(?:Animania|Colored)?)\s*\(\s*(?:this\s*,\s*)?([^)]*)\)", geometry):
        name, renderer_type, raw = match.groups()
        values = model_args(raw) if raw.strip() else []
        uv = (int(values[-2]), int(values[-1])) if len(values) >= 2 else (0, 0)
        parts[name] = Part(name, uv, colored=renderer_type == "ModelRendererColored")
    # ModelBase defaults to 64x32. Starting height at 64 silently doubled UV
    # space for the many legacy models that rely on that default.
    width, height = 64, 32
    for match in re.finditer(r"(?:this\.)?texture(Width|Height)\s*=\s*(\d+)", geometry):
        if match.group(1) == "Width":
            width = max(width, int(match.group(2)))
        else:
            height = max(height, int(match.group(2)))
    for match in re.finditer(r"(?:this\.)?(\w+)\.setTextureSize\s*\(([^)]*)\)", geometry):
        try:
            values = model_args(match.group(2)); width = max(width, int(values[0])); height = max(height, int(values[1]))
        except (ValueError, IndexError):
            pass
    for match in re.finditer(r"(?:this\.)?(\w+)\.addBox\s*\(([^)]*)\)", geometry):
        name = match.group(1)
        if name not in parts: continue
        try:
            values = tuple(model_args(match.group(2)))
            if len(values) >= 6: parts[name].boxes.append(values[:7])
        except ValueError:
            pass
    positioned: set[str] = set()
    for match in re.finditer(r"(?:this\.)?(\w+)\.setRotationPoint\s*\(([^)]*)\)", geometry):
        if match.group(1) in parts and match.group(1) not in positioned:
            try: parts[match.group(1)].pos = tuple(model_args(match.group(2))[:3])
            except ValueError: pass
            else: positioned.add(match.group(1))
    for match in re.finditer(r"(?:this\.)?(\w+)\.setOffset\s*\(([^)]*)\)", geometry):
        if match.group(1) in parts:
            try:
                parts[match.group(1)].offset = tuple(model_args(match.group(2))[:3])
            except ValueError: pass
    rotations: dict[str, list[float]] = {name: [0, 0, 0] for name in parts}
    axes = {"X": 0, "Y": 1, "Z": 2}
    for match in re.finditer(r"(?:this\.)?(\w+)\.rotateAngle([XYZ])\s*=\s*([^;]+);", defaults):
        name, axis, raw = match.groups()
        if name in rotations:
            try: rotations[name][axes[axis]] = number(raw)
            except ValueError: pass
    for match in re.finditer(r"(?:this\.)?set(?:RotateAngle|Rotation)\s*\(\s*(?:this\.)?(\w+)\s*,([^)]*)\)", defaults):
        if match.group(1) in rotations:
            try: rotations[match.group(1)] = model_args(match.group(2))[:3]
            except ValueError: pass
    for name, rotation in rotations.items(): parts[name].rot = tuple(rotation)
    sitting_pose: dict[str, PoseOverride] = {}
    sitting = method_body(text, re.compile(r"\bif\s*\(\s*sitting\b[^{;]*\)\s*\{"))
    if sitting is not None:
        sitting_rotations: dict[str, list[float | None]] = {}
        for match in re.finditer(r"(?:this\.)?(\w+)\.rotateAngle([XYZ])\s*=\s*([^;]+);", sitting):
            name, axis, raw = match.groups()
            if name not in parts:
                continue
            try:
                value = number(raw)
            except ValueError:
                continue
            sitting_rotations.setdefault(name, [None, None, None])[axes[axis]] = value
        sitting_positions: dict[str, tuple[float, float, float]] = {}
        for match in re.finditer(r"(?:this\.)?(\w+)\.setRotationPoint\s*\(([^)]*)\)", sitting):
            name = match.group(1)
            if name not in parts:
                continue
            try:
                values = tuple(model_args(match.group(2))[:3])
            except ValueError:
                continue
            if len(values) == 3:
                sitting_positions[name] = values
        changed = sitting_rotations.keys() | sitting_positions.keys()
        for name in parts:
            if name in changed:
                sitting_pose[name] = PoseOverride(
                    sitting_positions.get(name), tuple(sitting_rotations.get(name, [None, None, None])))
    # Extract the original walk cycle phase instead of guessing from the side
    # of the body. Quadrupeds animate diagonal pairs together; grouping all
    # left legs together produces an visibly incorrect pacing gait.
    for match in re.finditer(r"(?:this\.)?(\w+)\.rotateAngleX\s*=\s*(MathHelper\.cos\s*\([^;]+);", text):
        name, expression = match.groups()
        if name in parts:
            parts[name].gait_phase = 1 if re.search(r"Math\.PI|3\.14159", expression) else 0
    for match in re.finditer(r"(?:this\.)?(\w+)\.addChild\s*\(\s*(?:this\.)?(\w+)\s*\)", geometry):
        parent, child = match.groups()
        if parent in parts and child in parts and child not in parts[parent].children:
            parts[parent].children.append(child)
    for match in re.finditer(r"(?:this\.)?(\w+)\.mirror\s*=\s*true", geometry):
        if match.group(1) in parts: parts[match.group(1)].mirror = True
    private_parts: set[str] = set()
    for block in re.finditer(r"if\s*\(\s*AnimaniaConfig\.gameRules\.showParts\s*\)\s*\{(.*?)\}", text, re.S):
        for rendered in re.findall(r"(?:this\.)?(\w+)\.render\s*\(", block.group(1)):
            if rendered in parts:
                private_parts.add(rendered)
    if not parts:
        raise ValueError(f"no ModelRenderer geometry in {path}")
    return Model(path.stem, width, height, parts, private_parts, sitting_pose)


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
        for line in text.splitlines():
            match = re.search(pattern, line)
            if not match:
                continue
            entity, model = match.groups()
            remainder = line[match.end():]
            scale_match = re.search(r",\s*(-?[0-9]+(?:\.[0-9]+)?)f(?:\s*[,\)])", remainder)
            result[snake(entity)] = (model, float(scale_match.group(1)) if scale_match else 1.0)
        # Fox uses a dedicated renderer but the same Java model conversion path.
        result["male_fox"] = ("ModelFox", 1.0)
        result["female_fox"] = ("ModelFox", 0.9)
        result["puppy_fox"] = ("ModelFox", 0.5)
    return result


def catsdogs_translations(root: Path) -> dict[str, tuple[float, float, float]]:
    handler = root / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/CatsDogsAddonRenderHandler.java"
    text = handler.read_text(encoding="utf-8", errors="replace")
    result: dict[str, tuple[float, float, float]] = {}
    # RenderCatGeneric has no per-factory translation; record its explicit
    # identity transform so every Cats & Dogs registry ID remains audited.
    for entity in re.findall(
        r"Entity(\w+)\.class\s*,\s*new\s+RenderCatGeneric\.Factory\s*\(", text
    ):
        result[snake(entity)] = (0.0, 0.0, 0.0)
    pattern = re.compile(
        r"Entity(\w+)\.class\s*,\s*new\s+RenderDogGeneric\.Factory\s*\(\s*new\s+Model\w+\s*\(\s*\)\s*,"
        r"\s*r\([^)]*\)\s*,\s*r\([^)]*\)\s*,\s*[^,]+,\s*-?[0-9]+(?:\.[0-9]+)?f"
        r"(?:\s*,\s*(-?[0-9]+(?:\.[0-9]+)?)\s*,\s*(-?[0-9]+(?:\.[0-9]+)?)\s*,\s*(-?[0-9]+(?:\.[0-9]+)?))?",
        re.S,
    )
    for match in pattern.finditer(text):
        entity, x, y, z = match.groups()
        result[snake(entity)] = tuple(float(value) if value is not None else 0.0 for value in (x, y, z))
    for role in ("male", "female", "puppy"):
        result[f"{role}_fox"] = (0.0, 0.1, 0.0)
    return result


def emit_part(lines: list[str], model: Model, part: Part, variable: str, indent: str,
              inherited_offset: tuple[float, float, float] = (0, 0, 0)) -> None:
    builder = "CubeListBuilder.create()"
    if part.mirror: builder += ".mirror()"
    for box in part.boxes:
        # 1.12 ModelRendererAnimania is T(pivot) * R(rotation) * T(offset).
        # A LayerDefinition has no post-rotation part offset, so move each
        # cube locally and add the parent's offset to the child's pivot below.
        local_box = (box[0] + part.offset[0], box[1] + part.offset[1], box[2] + part.offset[2], *box[3:])
        builder += f".texOffs({part.uv[0]}, {part.uv[1]}).addBox({', '.join(f(v) for v in local_box[:6])}"
        if len(box) == 7: builder += f", new CubeDeformation({f(box[6])})"
        builder += ")"
    x = part.pos[0] + inherited_offset[0]
    y = part.pos[1] + inherited_offset[1]
    z = part.pos[2] + inherited_offset[2]
    rx, ry, rz = part.rot
    pose = f"PartPose.offsetAndRotation({f(x)}, {f(y)}, {f(z)}, {f(rx)}, {f(ry)}, {f(rz)})"
    safe = snake(part.name)
    lines.append(f'{indent}PartDefinition {safe} = {variable}.addOrReplaceChild("{safe}", {builder}, {pose});')
    for child in part.children:
        emit_part(lines, model, model.parts[child], safe, indent, part.offset)


def animation_profile(model: Model) -> tuple[list[str], list[str], list[str], list[str], list[str], list[str], list[str], list[str]]:
    children = {child for part in model.parts.values() for child in part.children}
    entries: list[tuple[str, str, tuple[str, ...]]] = []

    def walk(name: str, path: tuple[str, ...], ancestors: tuple[str, ...]) -> None:
        safe = snake(name)
        current = path + (safe,)
        entries.append(("/".join(current), safe, ancestors))
        for child in model.parts[name].children:
            walk(child, current, ancestors + (safe,))

    for name in model.parts:
        if name not in children:
            walk(name, (), ())

    def roots_with(token: str) -> list[str]:
        def is_part_token(name: str) -> bool:
            # `detail` ends in `tail`; matching arbitrary substrings made an
            # upper jaw animate as a tail. A model-part token is either the
            # full name, an underscore-delimited extension, or a numeric
            # sequence such as tail2.
            suffix = name[len(token):] if name.startswith(token) else None
            return suffix == "" or (suffix is not None and (suffix.startswith("_") or suffix.isdigit()))

        return [path for path, name, ancestors in entries
                if is_part_token(name) and not any(is_part_token(ancestor) for ancestor in ancestors)]

    heads = roots_with("head")
    if not heads: heads = roots_with("neck")[:1]
    limbs = [(path, name) for path, name, ancestors in entries
             if any(token in name for token in ("leg", "foot", "arm"))
             and not any(any(token in ancestor for token in ("leg", "foot", "arm")) for ancestor in ancestors)]

    def side(name: str, left: bool) -> bool:
        # Do not use a raw "_l" substring: every snake-cased "_leg"
        # contains it, which previously classified right legs as left legs.
        word = "left" if left else "right"
        suffix = "l" if left else "r"
        return (re.search(rf"(?:^|_){word}(?:_|$)", name) is not None
                or re.search(rf"(?:__|_){suffix}$", name) is not None
                or re.search(rf"(?:leg|foot|arm){suffix}(?:_|$)", name) is not None)

    part_by_safe_name = {snake(part.name): part for part in model.parts.values()}
    phase_a = [path for path, name in limbs if part_by_safe_name.get(name) is not None
               and part_by_safe_name[name].gait_phase == 0]
    phase_b = [path for path, name in limbs if part_by_safe_name.get(name) is not None
               and part_by_safe_name[name].gait_phase == 1]
    unassigned = [(path, name) for path, name in limbs
                  if part_by_safe_name.get(name) is None or part_by_safe_name[name].gait_phase is None]
    # Old models without an explicit cosine assignment still get a stable
    # diagonal fallback: back-left/front-right versus back-right/front-left.
    for index, (path, name) in enumerate(unassigned):
        left_side = side(name, True)
        right_side = side(name, False)
        front = "front" in name
        back = "back" in name or "rear" in name
        if (left_side and back) or (right_side and front):
            phase_a.append(path)
        elif (right_side and back) or (left_side and front):
            phase_b.append(path)
        elif index % 2 == 0:
            phase_a.append(path)
        else:
            phase_b.append(path)
    tails = roots_with("tail")
    wings = roots_with("wing")
    bodies = [path for path, name, ancestors in entries
              if "body" in name and not any("body" in ancestor for ancestor in ancestors)]
    private_names = {snake(name) for name in model.private_parts}
    private_parts = [path for path, name, ancestors in entries if name in private_names]
    colored_names = {snake(name) for name, part in model.parts.items() if part.colored}
    colored_parts = [path for path, name, ancestors in entries if name in colored_names]
    return heads[:2], phase_a[:8], phase_b[:8], tails[:2], wings[:2], bodies[:2], private_parts, colored_parts


def model_part_paths(model: Model) -> dict[str, str]:
    children = {child for part in model.parts.values() for child in part.children}
    paths: dict[str, str] = {}

    def walk(name: str, prefix: tuple[str, ...]) -> None:
        current = prefix + (snake(name),)
        paths[name] = "/".join(current)
        for child in model.parts[name].children:
            walk(child, current)

    for name in model.parts:
        if name not in children:
            walk(name, ())
    return paths


def java_optional(value: float | None) -> str:
    return "Float.NaN" if value is None else f(value)


def sitting_pose_java(model: Model) -> str:
    if not model.sitting_pose:
        return "LegacyPoseDefinition.EMPTY"
    paths = model_part_paths(model)
    entries: list[str] = []
    parent_by_child = {
        child: parent.name for parent in model.parts.values() for child in parent.children
    }
    for name, override in model.sitting_pose.items():
        if name not in paths:
            raise ValueError(f"{model.name}: sitting pose references unreachable part {name}")
        if override.pos is None:
            position = (None, None, None)
        else:
            # The parent ModelRendererAnimania offset is inherited before the
            # child's rotation point in 1.12; mirror emit_part's conversion.
            parent = parent_by_child.get(name)
            inherited = model.parts[parent].offset if parent is not None else (0.0, 0.0, 0.0)
            position = tuple(override.pos[index] + inherited[index] for index in range(3))
        values = (*position, *override.rot)
        entries.append(
            f'new LegacyPartPose("{paths[name]}", {", ".join(java_optional(value) for value in values)})'
        )
    return "new LegacyPoseDefinition(" + ", ".join(entries) + ")"


def java_array(values: list[str]) -> str:
    return "new String[]{" + ", ".join(f'"{value}"' for value in values) + "}"


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
             "import com.animania.client.model.LegacyAnimationProfile;",
             "import com.animania.client.model.LegacyPartPose;",
             "import com.animania.client.model.LegacyPoseDefinition;",
             "import com.animania.client.model.LegacyRenderTransform;", "",
             f"public final class {class_name} {{", "    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();",
             "    static {"]
    for entity in ids:
        lines.append(f'        LAYERS.put("{entity}", new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("{MOD_IDS[module]}", "{entity}"), "main"));')
    lines += ["    }", f"    private {class_name}() {{}}", "    public static LayerDefinition create(String id) {", "        return switch (id) {"]
    reverse: dict[str, list[str]] = {}
    for entity in ids: reverse.setdefault(mapping[entity][0], []).append(entity)
    for model_name, entity_ids in reverse.items():
        labels = ", ".join(f'"{entity}"' for entity in entity_ids)
        lines.append(f"            case {labels} -> {snake(model_name)}();")
    lines += ['            default -> throw new IllegalArgumentException("Unknown legacy model " + id);', "        };", "    }"]
    lines += ["    public static LegacyAnimationProfile profile(String id) {", "        return switch (id) {"]
    for model_name, entity_ids in reverse.items():
        labels = ", ".join(f'"{entity}"' for entity in entity_ids)
        profile = animation_profile(models[model_name])
        constructor = ", ".join(java_array(values) for values in profile)
        lines.append(f"            case {labels} -> new LegacyAnimationProfile({constructor});")
    lines += ["            default -> LegacyAnimationProfile.EMPTY;", "        };", "    }"]
    if module == "catsdogs":
        lines += ["    public static LegacyPoseDefinition sittingPose(String id) {", "        return switch (id) {"]
        for model_name, entity_ids in reverse.items():
            labels = ", ".join(f'"{entity}"' for entity in entity_ids)
            lines.append(f"            case {labels} -> {sitting_pose_java(models[model_name])};")
        lines += ["            default -> LegacyPoseDefinition.EMPTY;", "        };", "    }"]
        translations = catsdogs_translations(root)
        missing_translations = [entity for entity in ids if entity not in translations]
        if missing_translations:
            raise SystemExit(f"catsdogs: missing renderer translations: {missing_translations}")
        lines += ["    public static LegacyRenderTransform transform(String id) {", "        return switch (id) {"]
        by_translation: dict[tuple[float, float, float], list[str]] = {}
        for entity in ids:
            by_translation.setdefault(translations[entity], []).append(entity)
        for (x, y, z), entity_ids in by_translation.items():
            labels = ", ".join(f'"{entity}"' for entity in entity_ids)
            lines.append(f"            case {labels} -> new LegacyRenderTransform({f(x)}, {f(y)}, {f(z)});")
        lines += ["            default -> LegacyRenderTransform.EMPTY;", "        };", "    }"]
    lines += ["    public static float scale(String id) {", "        return switch (id) {"]
    by_scale: dict[float, list[str]] = {}
    for entity in ids: by_scale.setdefault(mapping[entity][1], []).append(entity)
    for scale, entity_ids in by_scale.items():
        labels = ", ".join(f'"{entity}"' for entity in entity_ids)
        lines.append(f"            case {labels} -> {f(scale)};")
    lines += ["            default -> 1.0F;", "        };", "    }"]
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


def emit_base_facilities(root: Path) -> None:
    """Convert the four Base Java-rendered facilities/items that are not entities."""
    model_root = root / "upstream/Animania-1.12/src/main/java/com/animania/client/models"
    sources = {
        "salt_lick": model_root / "blocks/ModelSaltLick.java",
        "nest": model_root / "ModelNest.java",
        "trough": model_root / "ModelTrough.java",
        "water_bottle": model_root / "ModelWaterBottle.java",
    }
    models = {key: parse_model(path) for key, path in sources.items()}
    class_name = "BaseLegacyModelLayers"
    lines = ["package com.animania.client.model;", "",
             "// Generated from the pinned LGPL-3.0 Animania 1.12 Java models; do not edit by hand.",
             "import java.util.LinkedHashMap;", "import java.util.Map;",
             "import net.minecraft.client.model.geom.ModelLayerLocation;", "import net.minecraft.client.model.geom.PartPose;",
             "import net.minecraft.client.model.geom.builders.CubeDeformation;", "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
             "import net.minecraft.client.model.geom.builders.LayerDefinition;", "import net.minecraft.client.model.geom.builders.MeshDefinition;",
             "import net.minecraft.client.model.geom.builders.PartDefinition;", "import net.minecraft.resources.ResourceLocation;", "",
             f"public final class {class_name} {{", "    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();",
             "    static {"]
    for key in models:
        lines.append(f'        LAYERS.put("{key}", new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("animania", "legacy/{key}"), "main"));')
    lines += ["    }", f"    private {class_name}() {{}}", "    public static LayerDefinition create(String id) {", "        return switch (id) {"]
    for key in models:
        lines.append(f'            case "{key}" -> {key}();')
    lines += ['            default -> throw new IllegalArgumentException("Unknown Base legacy model " + id);', "        };", "    }"]
    for key, model in models.items():
        lines += [f"    private static LayerDefinition {key}() {{", "        MeshDefinition mesh = new MeshDefinition();", "        PartDefinition root = mesh.getRoot();"]
        children = {child for part in model.parts.values() for child in part.children}
        for part in model.parts.values():
            if part.name not in children:
                emit_part(lines, model, part, "root", "        ")
        lines += [f"        return LayerDefinition.create(mesh, {model.width}, {model.height});", "    }"]
    lines.append("}")
    output = root / "base/src/main/java/com/animania/client/model/BaseLegacyModelLayers.java"
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("base", len(models), output)


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--modules", nargs="+", choices=("base", "farm", "extra", "catsdogs"),
                        help="only regenerate the selected model-layer modules")
    opts = parser.parse_args()
    modules = opts.modules or ("base", "farm", "extra", "catsdogs")
    if "base" in modules:
        emit_base_facilities(opts.root)
    for module in ("farm", "extra", "catsdogs"):
        if module in modules:
            emit(opts.root, module)


if __name__ == "__main__": main()
