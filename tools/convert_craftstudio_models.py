"""Convert archived CraftStudio JSON models into native LayerDefinition Java sources."""
from __future__ import annotations

import argparse
import json
import math
import re
import struct
from pathlib import Path

MODULES = {
    "base": ("animania", "BaseNativeModelLayers", "player-craftstudio"),
    "farm": ("animania_farm", "FarmNativeModelLayers", "farm-craftstudio"),
    "extra": ("animania_extra", "ExtraNativeModelLayers", "extra-craftstudio"),
    "catsdogs": ("animania_catsdogs", "CatsDogsNativeModelLayers", "catsdogs-craftstudio"),
}
KNOWN_TEXTURE_SIZES = {
    "model_bee_hive": (128, 64), "model_wild_hive": (128, 64),
    "model_cart": (128, 128), "model_cart_chest": (128, 128),
    "model_tiller": (128, 64), "model_wagon": (256, 128),
    "model_hamster_wheel": (64, 32), "hamster": (64, 32),
    "model_cat_bed_1": (64, 32), "model_cat_bed_2": (64, 32),
    "model_cat_tower": (128, 128), "model_dog_pillow": (128, 128),
    "model_litter_box": (64, 64), "model_dog_house": (64, 64),
    "model_pet_bowl": (64, 32), "model_ragdoll": (128, 64),
    "player": (64, 64), "player_sit": (64, 64),
}

EXACT_RUNTIME_MODELS = {
    "farm": {"model_bee_hive", "model_wild_hive"},
    "extra": {"model_hamster_wheel"},
    "catsdogs": {"model_cat_bed_1", "model_cat_bed_2", "model_cat_tower", "model_dog_house",
                 "model_dog_pillow", "model_litter_box", "model_pet_bowl"},
}


def safe(value: str) -> str:
    value = re.sub(r"[^a-zA-Z0-9_]", "_", value)
    value = re.sub(r"(?<!^)(?=[A-Z])", "_", value).lower()
    return value if not value[:1].isdigit() else "part_" + value


def fl(value: float) -> str:
    if abs(value) < 1e-7: value = 0.0
    out = f"{value:.6f}".rstrip("0").rstrip(".")
    if "." not in out: out += ".0"
    return out + "F"


def legacy_euler_to_modelpart(rx: float, ry: float, rz: float) -> tuple[float, float, float]:
    """Convert CraftStudioAPI's quaternion Euler order to ModelPart Euler angles."""
    cx, cy, cz = math.cos(rx / 2), math.cos(ry / 2), math.cos(rz / 2)
    sx, sy, sz = math.sin(rx / 2), math.sin(ry / 2), math.sin(rz / 2)
    qw = cx * cy * cz + sx * sy * sz
    qx = sx * cy * cz + cx * sy * sz
    qy = cx * sy * cz - sx * cy * sz
    qz = cx * cy * sz - sx * sy * cz
    x = math.atan2(2 * (qw * qx + qy * qz), 1 - 2 * (qx * qx + qy * qy))
    sin_y = max(-1.0, min(1.0, 2 * (qw * qy - qz * qx)))
    y = math.asin(sin_y)
    z = math.atan2(2 * (qw * qz + qx * qy), 1 - 2 * (qy * qy + qz * qz))
    return x, y, z


def emit_node(lines: list[str], node: dict, parent: str, used: set[str], indent: str = "        ") -> None:
    base = safe(str(node.get("name", "part")))
    name = base
    suffix = 2
    while name in used:
        name = f"{base}_{suffix}"; suffix += 1
    used.add(name)
    size = [float(v) for v in node.get("size", [0, 0, 0])]
    raw_offset = [float(v) for v in node.get("offsetFromPivot", [0, 0, 0])]
    raw_position = [float(v) for v in node.get("position", [0, 0, 0])]
    raw_rotation = [float(v) for v in node.get("rotation", [0, 0, 0])]
    # This is the exact coordinate normalization performed by
    # CraftStudioAPI 1.0.1.95's CSJsonReader before ModelCraftStudio sees a
    # node. Root Y is additionally anchored to the legacy 24-pixel entity
    # origin. Omitting this conversion inverted every native prop model.
    offset = [raw_offset[0], -raw_offset[1], -raw_offset[2]]
    position = [raw_position[0], (24.0 - raw_position[1]) if parent == "root" else -raw_position[1],
                -raw_position[2]]
    source_rotation = [math.radians(raw_rotation[0]), math.radians(-raw_rotation[1]),
                       math.radians(-raw_rotation[2])]
    rotation = list(legacy_euler_to_modelpart(*source_rotation))
    uv = [int(v) for v in node.get("texOffset", [0, 0])]
    builder = "CubeListBuilder.create()"
    # CraftStudio intentionally uses zero-thickness cubes for wings, spokes,
    # webs and other textured planes. CubeListBuilder supports those planes;
    # dropping them made the hamster wheel and several Farm props incomplete.
    if any(value > 0 for value in size) and all(value >= 0 for value in size):
        origin = [offset[i] - size[i] / 2.0 for i in range(3)]
        builder += f".texOffs({uv[0]}, {uv[1]}).addBox({', '.join(fl(v) for v in origin + size)})"
    pose = f"PartPose.offsetAndRotation({', '.join(fl(v) for v in position + rotation)})"
    lines.append(f'{indent}PartDefinition {name} = {parent}.addOrReplaceChild("{name}", {builder}, {pose});')
    for child in node.get("children", []):
        emit_node(lines, child, name, used, indent)


def emit_module(root: Path, module: str, modid: str, class_name: str, archive: str) -> None:
    if module == "base":
        model_root = root / "upstream/Animania-1.12/src/main/resources/assets/player_anim/craftstudio/models"
        package = "com.animania.client.model"
        output_root = root / "base/src/main/java/com/animania/client/model"
    else:
        model_root = root / "legacy-archive" / archive / "models"
        package = f"com.animania.{module}.client.model"
        output_root = root / module / "src/main/java/com/animania" / module / "client/model"
    files = sorted(model_root.rglob("*.csjsmodel"))
    lines = [f"package {package};", "",
             "// Generated from archived LGPL-3.0 legacy native JSON; no legacy native runtime dependency.",
             "import java.util.LinkedHashMap;", "import java.util.Map;",
             "import net.minecraft.client.model.geom.ModelLayerLocation;", "import net.minecraft.client.model.geom.PartPose;",
             "import net.minecraft.client.model.geom.builders.CubeListBuilder;", "import net.minecraft.client.model.geom.builders.LayerDefinition;",
             "import net.minecraft.client.model.geom.builders.MeshDefinition;", "import net.minecraft.client.model.geom.builders.PartDefinition;",
             "import net.minecraft.resources.ResourceLocation;", "", f"public final class {class_name} {{",
             "    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();", "    static {"]
    models: list[tuple[str, dict]] = []
    for path in files:
        key = path.stem
        models.append((key, json.loads(path.read_text(encoding="utf-8"))))
        lines.append(f'        LAYERS.put("{key}", new ModelLayerLocation(new ResourceLocation("{modid}", "native/{key}"), "main"));')
    lines += ["    }", f"    private {class_name}() {{}}", "    public static LayerDefinition create(String id) {", "        return switch (id) {"]
    for key, _ in models: lines.append(f'            case "{key}" -> {safe(key)}();')
    lines += ['            default -> throw new IllegalArgumentException("Unknown legacy native model " + id);', "        };", "    }"]
    for key, data in models:
        width, height = KNOWN_TEXTURE_SIZES.get(key, (128, 128))
        lines += [f"    private static LayerDefinition {safe(key)}() {{", "        MeshDefinition mesh = new MeshDefinition();", "        PartDefinition root = mesh.getRoot();"]
        used: set[str] = set()
        for node in data.get("tree", []): emit_node(lines, node, "root", used)
        lines += [f"        return LayerDefinition.create(mesh, {width}, {height});", "    }"]
    lines.append("}")
    output = output_root / f"{class_name}.java"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    emit_exact_runtime_models(root, module, model_root, models, package, output_root,
                              class_name.replace("NativeModelLayers", "LegacyPropModels"))
    emit_animations(root, module, archive, class_name.replace("ModelLayers", "Animations"))
    print(module, len(models), output)


def legacy_vertices(node: dict) -> list[list[float]]:
    """Return final cube-local vertices after CSJsonReader's stretch and offset transforms."""
    size = [float(v) for v in node.get("size", [0, 0, 0])]
    raw_offset = [float(v) for v in node.get("offsetFromPivot", [0, 0, 0])]
    offset = [raw_offset[0], -raw_offset[1], -raw_offset[2]]
    custom = node.get("vertexCoords")
    if custom is not None:
        # CSJsonReader reorders CraftStudio's exported corners, flips Y/Z,
        # normalizes them, then CSModelRenderer restores the scale. Combining
        # those operations gives these exact final local coordinates.
        order = [3, 2, 1, 0, 6, 7, 4, 5]
        vertices = []
        for index in order:
            raw = [float(v) for v in custom[index]]
            vertices.append([raw[0] + offset[0], -raw[1] + offset[1], -raw[2] + offset[2]])
        return vertices
    transformed_size = [size[0], -size[1], -size[2]]
    low = [-value / 2.0 for value in transformed_size]
    high = [low[i] + transformed_size[i] for i in range(3)]
    raw_vertices = [
        [low[0], low[1], low[2]], [high[0], low[1], low[2]],
        [high[0], high[1], low[2]], [low[0], high[1], low[2]],
        [low[0], low[1], high[2]], [high[0], low[1], high[2]],
        [high[0], high[1], high[2]], [low[0], high[1], high[2]],
    ]
    return [[vertex[i] + offset[i] for i in range(3)] for vertex in raw_vertices]


def legacy_texture_rects(node: dict) -> list[list[int]]:
    u, v = [int(value) for value in node.get("texOffset", [0, 0])]
    x, y, z = [int(float(value)) for value in node.get("size", [0, 0, 0])]
    return [
        [u + z + x + z, v + z + y, u + z + x, v + z],
        [u + z, v + z + y, u, v + z],
        [u + z + x, v, u + z + x + x, v + z],
        [u + z, v, u + z + x, v + z],
        [u + z + x + z + x + z, v + z + y, u + z + x + z, v + z],
        [u + z + x, v + z + y, u + z, v + z],
    ]


def legacy_faces_flipped(vertices: list[list[float]]) -> bool:
    """Reproduce CSModelBox.checkBlockForShadow's three parity flips."""
    flips = 0
    if vertices[1][0] - vertices[0][0] < 0: flips += 1
    if vertices[3][1] - vertices[0][1] > 0: flips += 1
    if vertices[4][2] - vertices[0][2] > 0: flips += 1
    return flips % 2 == 1


def java_float_matrix(values: list[list[float]]) -> str:
    rows = ", ".join("{" + ", ".join(fl(value) for value in row) + "}" for row in values)
    return "new float[][]{" + rows + "}"


def java_int_matrix(values: list[list[int]]) -> str:
    rows = ", ".join("{" + ", ".join(str(value) for value in row) + "}" for row in values)
    return "new int[][]{" + rows + "}"


def emit_runtime_node(lines: list[str], node: dict, is_root: bool, width: int, height: int,
                      used: set[str], parent_offset: list[float] | None = None) -> tuple[str, str]:
    base = safe(str(node.get("name", "part")))
    name = base
    suffix = 2
    while name in used:
        name = f"{base}_{suffix}"
        suffix += 1
    used.add(name)
    raw_offset = [float(v) for v in node.get("offsetFromPivot", [0, 0, 0])]
    offset = [raw_offset[0], -raw_offset[1], -raw_offset[2]]
    children = [emit_runtime_node(lines, child, False, width, height, used, offset)
                for child in node.get("children", [])]
    raw_position = [float(v) for v in node.get("position", [0, 0, 0])]
    position = [raw_position[0], 24.0 - raw_position[1] if is_root else -raw_position[1], -raw_position[2]]
    if parent_offset is not None:
        position = [position[i] + parent_offset[i] for i in range(3)]
    raw_rotation = [float(v) for v in node.get("rotation", [0, 0, 0])]
    source_rotation = [math.radians(raw_rotation[0]), math.radians(-raw_rotation[1]), math.radians(-raw_rotation[2])]
    rotation = list(legacy_euler_to_modelpart(*source_rotation))
    child_map = "Map.of()" if not children else "Map.ofEntries(" + ", ".join(
        f'Map.entry("{child_name}", {child_var})' for child_name, child_var in children) + ")"
    variable = f"part_{len(used)}_{name}"
    raw_vertices = legacy_vertices(node)
    vertices = java_float_matrix(raw_vertices)
    rects = java_int_matrix(legacy_texture_rects(node))
    pose = f"PartPose.offsetAndRotation({', '.join(fl(value) for value in position + rotation)})"
    lines.append(f"        ModelPart {variable} = LegacyMeshModel.part({pose},")
    flipped = str(legacy_faces_flipped(raw_vertices)).lower()
    lines.append(f"                new LegacyMeshCube({vertices}, {rects}, {width}, {height}, {flipped}), {child_map});")
    return name, variable


def emit_exact_runtime_models(root: Path, module: str, model_root: Path, models: list[tuple[str, dict]],
                              package: str, output_root: Path, class_name: str) -> None:
    selected = EXACT_RUNTIME_MODELS.get(module)
    if not selected:
        return
    model_map = dict(models)
    missing = selected.difference(model_map)
    if missing:
        raise RuntimeError(f"Missing exact runtime models for {module}: {sorted(missing)}")
    lines = [f"package {package};", "", "// Generated from the exact CraftStudio cuboid topology and UV layout.",
             "import com.animania.client.model.LegacyMeshCube;",
             "import com.animania.client.model.LegacyMeshModel;",
             "import java.util.Map;", "import net.minecraft.client.model.geom.ModelPart;",
             "import net.minecraft.client.model.geom.PartPose;", "", f"public final class {class_name} {{",
             f"    private {class_name}() {{ }}", "", "    public static ModelPart create(String id) {",
             "        return switch (id) {"]
    for key in sorted(selected):
        lines.append(f'            case "{key}" -> {safe(key)}();')
    lines += ['            default -> throw new IllegalArgumentException("Unknown exact CraftStudio model " + id);',
              "        };", "    }"]
    for key in sorted(selected):
        data = model_map[key]
        width, height = KNOWN_TEXTURE_SIZES[key]
        lines += ["", f"    private static ModelPart {safe(key)}() {{"]
        used: set[str] = set()
        roots = [emit_runtime_node(lines, node, True, width, height, used) for node in data.get("tree", [])]
        entries = ", ".join(f'Map.entry("{name}", {variable})' for name, variable in roots)
        lines.append(f"        return LegacyMeshModel.root(Map.ofEntries({entries}));")
        lines.append("    }")
    lines.append("}")
    output = output_root / f"{class_name}.java"
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def emit_animations(root: Path, module: str, archive: str, class_name: str) -> None:
    if module == "base":
        animation_root = root / "upstream/Animania-1.12/src/main/resources/assets/player_anim/craftstudio/animations"
        package = "com.animania.client.model"
        output_root = root / "base/src/main/java/com/animania/client/model"
    else:
        animation_root = root / "legacy-archive" / archive / "animations"
        package = f"com.animania.{module}.client.model"
        output_root = root / module / "src/main/java/com/animania" / module / "client/model"
    files = sorted(animation_root.rglob("*.csjsmodelanim"))
    lines = [f"package {package};", "",
             "// Generated native AnimationDefinitions from archived legacy native keyframes.",
             "import java.util.LinkedHashMap;", "import java.util.Map;",
             "import net.minecraft.client.animation.AnimationChannel;", "import net.minecraft.client.animation.AnimationDefinition;",
             "import net.minecraft.client.animation.Keyframe;", "import static net.minecraft.client.animation.AnimationChannel.Interpolations.LINEAR;",
             "import static net.minecraft.client.animation.AnimationChannel.Targets.POSITION;",
             "import static net.minecraft.client.animation.AnimationChannel.Targets.ROTATION;",
             "import static net.minecraft.client.animation.KeyframeAnimations.degreeVec;",
             "import static net.minecraft.client.animation.KeyframeAnimations.posVec;", "",
             f"public final class {class_name} {{", "    public static final Map<String, AnimationDefinition> ALL = new LinkedHashMap<>();",
             "    static {"]
    for path in files:
        key = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        duration = float(data.get("duration", 1)) / 20.0
        lines.append(f"        ALL.put(\"{key}\", {safe(key)}());")
    lines += ["    }", f"    private {class_name}() {{}}"]
    for path in files:
        key = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        duration = max(0.05, float(data.get("duration", 1)) / 20.0)
        lines.append(f"    private static AnimationDefinition {safe(key)}() {{")
        lines.append(f"        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength({fl(duration)}).looping();")
        for raw_node, channels in data.get("nodeAnimations", {}).items():
            node = safe(raw_node)
            for source_key, target, vector in (("position", "POSITION", "posVec"), ("rotation", "ROTATION", "degreeVec")):
                values = channels.get(source_key, {})
                if not values: continue
                frames = []
                for tick, coords in sorted(values.items(), key=lambda item: float(item[0])):
                    time = float(tick) / 20.0
                    frames.append(f"new Keyframe({fl(time)}, {vector}({', '.join(fl(float(v)) for v in coords)}), LINEAR)")
                lines.append(f'        builder.addAnimation("{node}", new AnimationChannel({target}, {", ".join(frames)}));')
        lines += ["        return builder.build();", "    }"]
    lines.append("}")
    output = output_root / f"{class_name}.java"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    for module, values in MODULES.items(): emit_module(args.root, module, *values)


if __name__ == "__main__": main()
