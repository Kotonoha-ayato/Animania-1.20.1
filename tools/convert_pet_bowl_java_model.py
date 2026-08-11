"""Generate the active Cats & Dogs pet-bowl layer from its 1.12 Java model."""
from __future__ import annotations

import argparse
import re
from pathlib import Path

import convert_legacy_java_models as legacy


def assigned_rotations(body: str) -> dict[str, tuple[float, float, float]]:
    values: dict[str, list[float]] = {}
    axes = {"X": 0, "Y": 1, "Z": 2}
    for name, axis, raw in re.findall(
            r"(?:this\.)?(\w+)\.rotateAngle([XYZ])\s*=\s*([^;]+);", body):
        values.setdefault(name, [0.0, 0.0, 0.0])[axes[axis]] = legacy.number(raw)
    return {name: tuple(rotation) for name, rotation in values.items()}


def rendered_names(body: str) -> list[str]:
    return re.findall(r"(?:this\.)?(\w+)\.render(?:WithRotation)?\s*\(", body)


def generate(root: Path) -> None:
    source = root / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/blocks/ModelPetBowl.java"
    text = source.read_text(encoding="utf-8", errors="replace")
    model = legacy.parse_model(source)
    shell_body = legacy.method_body(text, re.compile(
        r"public\s+void\s+render\s*\(\s*Entity\b[^)]*\)\s*\{"))
    food_body = legacy.method_body(text, re.compile(
        r"public\s+void\s+renderFood\s*\([^)]*\)\s*\{"))
    if shell_body is None or food_body is None:
        raise SystemExit("missing active ModelPetBowl render methods")
    shell = rendered_names(shell_body)
    food = rendered_names(food_body)
    if len(shell) != 13 or len(food) != 71 or set(shell) & set(food):
        raise SystemExit(f"unexpected pet-bowl split: shell={len(shell)}, food={len(food)}")
    rotations = assigned_rotations(shell_body) | assigned_rotations(food_body)
    for name, rotation in rotations.items():
        model.parts[name].rot = legacy.legacy_euler_to_modelpart(*rotation)

    lines = [
        "package com.animania.catsdogs.client.model;", "",
        "// Generated from the active LGPL-3.0 Animania 1.12 ModelPetBowl Java renderer.",
        "import com.mojang.blaze3d.vertex.PoseStack;",
        "import com.mojang.blaze3d.vertex.VertexConsumer;",
        "import net.minecraft.client.model.geom.ModelLayerLocation;",
        "import net.minecraft.client.model.geom.ModelPart;",
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "import net.minecraft.resources.ResourceLocation;", "",
        "public final class CatsDogsPetBowlModel {",
        "    public static final ModelLayerLocation LAYER = new ModelLayerLocation(",
        "            ResourceLocation.fromNamespaceAndPath(\"animania_catsdogs\", \"pet_bowl_java\"), \"main\");",
        "    private final ModelPart root;", "",
        "    public CatsDogsPetBowlModel(ModelPart root) { this.root = root; }", "",
        "    public void renderShell(PoseStack pose, VertexConsumer consumer, int light, int overlay) {",
    ]
    for name in shell:
        lines.append(f'        root.getChild("{legacy.snake(name)}").render(pose, consumer, light, overlay);')
    lines += ["    }", "", "    public void renderFood(PoseStack pose, VertexConsumer consumer, int light, int overlay,",
              "                           float red, float green, float blue) {"]
    for name in food:
        lines.append(f'        root.getChild("{legacy.snake(name)}").render(pose, consumer, light, overlay, red, green, blue, 1.0F);')
    lines += ["    }", "", "    public static LayerDefinition create() {",
              "        MeshDefinition mesh = new MeshDefinition();", "        PartDefinition root = mesh.getRoot();"]
    for part in model.parts.values():
        legacy.emit_part(lines, model, part, "root", "        ")
    lines += [f"        return LayerDefinition.create(mesh, {model.width}, {model.height});", "    }", "}"]
    output = root / "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsPetBowlModel.java"
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"pet_bowl shell={len(shell)} food={len(food)} total={len(model.parts)} {output}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    generate(args.root.resolve())


if __name__ == "__main__":
    main()
