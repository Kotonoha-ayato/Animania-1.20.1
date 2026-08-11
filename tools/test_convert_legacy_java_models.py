"""Regression checks for the native 1.12 Java-model converter."""

from __future__ import annotations

import importlib.util
import math
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "convert_legacy_java_models", ROOT / "tools" / "convert_legacy_java_models.py"
)
assert SPEC is not None and SPEC.loader is not None
CONVERTER = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = CONVERTER
SPEC.loader.exec_module(CONVERTER)

PIVOT_SPEC = importlib.util.spec_from_file_location(
    "repair_zero_volume_pivots", ROOT / "tools" / "repair_zero_volume_pivots.py"
)
assert PIVOT_SPEC is not None and PIVOT_SPEC.loader is not None
PIVOT_REPAIR = importlib.util.module_from_spec(PIVOT_SPEC)
sys.modules[PIVOT_SPEC.name] = PIVOT_REPAIR
PIVOT_SPEC.loader.exec_module(PIVOT_REPAIR)


class LegacyJavaModelConverterTest(unittest.TestCase):
    def test_pet_bowl_delegating_constructor_keeps_all_shell_and_food_parts(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/blocks/ModelPetBowl.java"
        )
        self.assertEqual(84, len(model.parts))
        self.assertEqual(71, sum(part.colored for part in model.parts.values()))

    def test_fox_multi_axis_ears_are_converted_from_craftstudio_rotation_order(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs/ModelFox.java"
        )
        legacy_rotation = (-1.1021789132929232, -1.6230567205873627, 1.520239374352377)
        converted = CONVERTER.legacy_euler_to_modelpart(*legacy_rotation)
        self.assertEqual(converted, model.parts["ear_l"].rot)
        self.assertGreater(max(abs(a - b) for a, b in zip(converted, legacy_rotation)), 0.5)
        rx, ry, rz = legacy_rotation
        cx, cy, cz = (math.cos(value / 2) for value in (rx, ry, rz))
        sx, sy, sz = (math.sin(value / 2) for value in (rx, ry, rz))
        legacy_quaternion = (
            cx * cy * cz + sx * sy * sz,
            sx * cy * cz + cx * sy * sz,
            cx * sy * cz - sx * cy * sz,
            cx * cy * sz - sx * sy * cz,
        )
        mx, my, mz = converted
        cx, cy, cz = (math.cos(value / 2) for value in (mx, my, mz))
        sx, sy, sz = (math.sin(value / 2) for value in (mx, my, mz))
        modelpart_quaternion = (
            cx * cy * cz + sx * sy * sz,
            sx * cy * cz - cx * sy * sz,
            cx * sy * cz + sx * cy * sz,
            cx * cy * sz - sx * sy * cz,
        )
        for expected, actual in zip(legacy_quaternion, modelpart_quaternion):
            self.assertAlmostEqual(expected, actual, places=6)

    def test_vanilla_renderer_multi_axis_rotation_is_not_craftstudio_converted(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/client/model/goats/ModelBuckAlpine.java"
        )
        source_rotation = (0.398545, -0.2031062, 0.3211416)
        # This source part is a vanilla ModelRenderer; its Euler order already
        # matches ModelPart and must survive byte-for-byte numerically.
        self.assertFalse(model.parts["Ear_R"].animania_rotation)
        self.assertEqual(source_rotation, model.parts["Ear_R"].rot)

    def test_commented_angus_horns_are_not_geometry(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/client/model/cow/ModelCowAngus.java"
        )
        self.assertNotIn("Horn1", model.parts)
        self.assertNotIn("Horn2", model.parts)

    def test_field_initialized_parent_bone_keeps_its_geometry_and_children(self) -> None:
        # ModelCow declares its head at field scope instead of in the
        # constructor.  Losing that declaration drops the entire head and
        # flattens its facial children at the root in the generated layer.
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/client/model/cow/ModelCow.java"
        )
        self.assertIn("head", model.parts)
        self.assertEqual((0.0, 5.0, -12.0), model.parts["head"].pos)
        self.assertEqual([(-4.0, -4.0, -3.0, 8.0, 8.0, 6.0)], model.parts["head"].boxes)
        self.assertEqual(["Horn1", "Horn2", "Snout", "EarL", "EarLa", "EarR", "EarRa"],
                         model.parts["head"].children)

    def test_all_adult_cow_layers_keep_the_field_initialized_head_hierarchy(self) -> None:
        target = (ROOT / "farm/src/main/java/com/animania/farm/client/model/FarmLegacyModelLayers.java")
        text = target.read_text(encoding="utf-8")
        sources = (
            ("model_cow", "ModelCow.java"),
            ("model_cow_angus", "ModelCowAngus.java"),
            ("model_cow_hereford", "ModelCowHereford.java"),
            ("model_cow_longhorn", "ModelCowLonghorn.java"),
        )
        source_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/client/model/cow"
        for method, source_name in sources:
            start = text.index(f"    private static LayerDefinition {method}()")
            end = text.find("    private static LayerDefinition ", start + 1)
            generated = text[start:end if end >= 0 else len(text)]
            legacy = CONVERTER.parse_model(source_root / source_name)
            self.assertIn('PartDefinition head = root.addOrReplaceChild("head"', generated, method)
            for child in legacy.parts["head"].children:
                self.assertIn(f'head.addOrReplaceChild("{CONVERTER.snake(child)}"', generated,
                              f"{method} lost head child {child}")

    def test_adult_cattle_converter_corrects_legacy_same_side_pacing(self) -> None:
        source_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/client/model/cow"
        expected = {
            "ModelBull.java": (["leg0", "leg2"], ["leg1", "leg3"]),
            "ModelBullAngus.java": (["leg0", "leg2"], ["leg1", "leg3"]),
            "ModelBullHereford.java": (["leg0", "leg2"], ["leg1", "leg3"]),
            "ModelBullLonghorn.java": (["leg0", "leg2"], ["leg1", "leg3"]),
            "ModelCow.java": (["leg1", "leg3"], ["leg2", "leg4"]),
            "ModelCowAngus.java": (["leg1", "leg3"], ["leg2", "leg4"]),
            "ModelCowHereford.java": (["leg1", "leg3"], ["leg2", "leg4"]),
            "ModelCowLonghorn.java": (["leg1", "leg3"], ["leg2", "leg4"]),
        }
        for file_name, (phase_a, phase_b) in expected.items():
            source = source_root / file_name
            model = CONVERTER.parse_model(source)
            _, actual_a, actual_b, *_ = CONVERTER.animation_profile(model, source)
            self.assertEqual(phase_a, actual_a, file_name)
            self.assertEqual(phase_b, actual_b, file_name)

    def test_generated_layers_contain_no_renderable_zero_volume_pivots(self) -> None:
        pattern = PIVOT_REPAIR.ZERO_VOLUME_BOX
        layers = [
            *ROOT.glob("*/src/main/java/**/**LegacyModelLayers.java"),
        ]
        self.assertTrue(layers)
        for layer in layers:
            self.assertIsNone(pattern.search(layer.read_text(encoding="utf-8")), layer)

    def test_zero_volume_pivot_repair_preserves_flat_detail_planes(self) -> None:
        text = (
            "CubeListBuilder.create().texOffs(1, 2).addBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)\n"
            "CubeListBuilder.create().texOffs(1, 2).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 0.0F, 4.0F)\n"
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "Layer.java"
            path.write_text(text, encoding="utf-8")
            self.assertEqual(1, PIVOT_REPAIR.repair(path))
            repaired = path.read_text(encoding="utf-8")
        self.assertEqual("CubeListBuilder.create()", repaired.splitlines()[0])
        self.assertIn("addBox(-1.0F, 0.0F, -2.0F, 2.0F, 0.0F, 4.0F)", repaired)

    def test_every_field_initialized_renderer_is_retained_across_all_animal_modules(self) -> None:
        addon_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons"
        sources = [
            *sorted((addon_root / "farm/client").rglob("Model*.java")),
            *sorted((addon_root / "extra/client").rglob("Model*.java")),
            *sorted((addon_root / "catsdogs/client").rglob("Model*.java")),
        ]
        for source in sources:
            text = source.read_text(encoding="utf-8", errors="replace")
            text = CONVERTER.re.sub(r"/\*.*?\*/", "", text, flags=CONVERTER.re.S)
            text = CONVERTER.re.sub(r"//[^\n]*", "", text)
            expected = {match.group("name") for match in CONVERTER.FIELD_INITIALIZER.finditer(text)}
            if expected:
                model = CONVERTER.parse_model(source)
                self.assertTrue(expected <= set(model.parts),
                                f"{source.relative_to(ROOT)} dropped {expected - set(model.parts)}")

    def test_every_generated_animal_layer_keeps_source_nodes_and_parentage(self) -> None:
        """Prevent another flattened/lost-bone regression across animal layers.

        Property models intentionally live in the dedicated prop-model classes
        and are not included here.  Every generated animal layer must retain
        every parsed source node and attach it to the same parent (or to that
        parent's native offset node when the legacy model used one).
        """
        audited: dict[str, int] = {"farm": 0, "extra": 0, "catsdogs": 0}
        layer_names = {
            "farm": "FarmLegacyModelLayers.java",
            "extra": "ExtraLegacyModelLayers.java",
            "catsdogs": "CatsDogsLegacyModelLayers.java",
        }
        for module, layer_name in layer_names.items():
            source_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons" / module / "client"
            package = "catsdogs" if module == "catsdogs" else module
            layer = ROOT / module / "src/main/java/com/animania" / package / "client/model" / layer_name
            generated_layer = layer.read_text(encoding="utf-8")
            for source in sorted(source_root.rglob("Model*.java")):
                try:
                    model = CONVERTER.parse_model(source)
                except ValueError:
                    continue  # CraftStudio and delegated prop models
                signature = f"    private static LayerDefinition {CONVERTER.snake(model.name)}()"
                if signature not in generated_layer:
                    continue  # Dedicated prop-model class, not an animal layer
                start = generated_layer.index(signature)
                end = generated_layer.find("    private static LayerDefinition ", start + len(signature))
                generated = generated_layer[start:end if end >= 0 else len(generated_layer)]
                audited[module] += 1
                for part in model.parts.values():
                    self.assertIn(f"PartDefinition {CONVERTER.snake(part.name)} =", generated,
                                  f"{source.relative_to(ROOT)} lost {part.name}")
                    receiver = CONVERTER.snake(part.name)
                    if CONVERTER.uses_offset_node(part):
                        receiver += "_offset"
                    for child in part.children:
                        self.assertIn(
                            f'{receiver}.addOrReplaceChild("{CONVERTER.snake(child)}"', generated,
                            f"{source.relative_to(ROOT)} detached {part.name}/{child}",
                        )
        self.assertEqual({"farm": 55, "extra": 16, "catsdogs": 22}, audited)

    def test_detail_is_not_a_tail_token(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs/ModelCollie.java"
        )
        _, _, _, tails, _, _, _, _ = CONVERTER.animation_profile(model)
        self.assertTrue(tails)
        self.assertFalse(any("detail" in path for path in tails), tails)

    def test_dog_constructor_pose_does_not_absorb_runtime_sitting_pose(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs/ModelCollie.java"
        )
        body = model.parts["body"]
        lower_body = model.parts["lower_body"]
        self.assertEqual((0.0, 10.0, -5.0), body.pos)
        self.assertEqual((0.0, 0.6, -1.5), body.offset)
        self.assertAlmostEqual(-0.06981317007977318, body.rot[0])
        self.assertAlmostEqual(0.0, lower_body.rot[0])

    def test_labrador_uses_runtime_standing_pivot_not_editor_pose(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs/ModelLabrador.java"
        )
        self.assertEqual((0.0, 10.0, -5.0), model.parts["body"].pos)

    def test_every_dog_standing_pivot_respects_last_write_before_render(self) -> None:
        dog_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs"
        pattern = CONVERTER.re.compile(r"\bif\s*\(\s*!\s*sitting\b[^{;]*\)\s*\{")
        for path in sorted(dog_root.glob("Model*.java")):
            model = CONVERTER.parse_model(path)
            text = path.read_text(encoding="utf-8", errors="replace")
            text = CONVERTER.re.sub(r"/\*.*?\*/", "", text, flags=CONVERTER.re.S)
            text = CONVERTER.re.sub(r"//[^\n]*", "", text)
            standing = CONVERTER.method_body(text, pattern)
            if standing is None:
                continue
            setup = CONVERTER.method_body(
                text,
                CONVERTER.re.compile(
                    r"\b(?:public|private|protected)?\s*(?:final\s+)?void\s+setupAngles\s*\(\s*\)\s*\{"
                ),
            ) or ""
            setup_points = dict(CONVERTER.re.findall(
                r"(?:this\.)?(\w+)\.setRotationPoint\s*\(([^)]*)\)", setup
            ))
            for name, raw in CONVERTER.re.findall(
                r"(?:this\.)?(\w+)\.setRotationPoint\s*\(([^)]*)\)", standing
            ):
                if name in model.parts:
                    effective = setup_points.get(name, raw)
                    self.assertEqual(tuple(CONVERTER.args(effective)[:3]), model.parts[name].pos,
                                     f"{path.name}:{name}")

    def test_greyhound_tail_uses_setup_angles_pivot_not_overwritten_living_animation(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs/ModelGreyhound.java"
        )
        self.assertEqual((0.0, -2.4369, 6.0599), model.parts["tail"].pos)

    def test_offset_stays_as_a_native_node_between_rotation_and_children(self) -> None:
        parent = CONVERTER.Part("parent", boxes=[(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)],
                                pos=(10.0, 20.0, 30.0), offset=(0.5, -1.0, 2.0), children=["child"])
        child = CONVERTER.Part("child", boxes=[(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)],
                               pos=(4.0, 5.0, 6.0), offset=(-0.5, 1.0, -2.0))
        model = CONVERTER.Model("Synthetic", 64, 32, {"parent": parent, "child": child}, set())
        lines: list[str] = []
        CONVERTER.emit_part(lines, model, parent, "root", "")
        emitted = "\n".join(lines)
        self.assertIn("addBox(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F)", emitted)
        self.assertIn("PartPose.offsetAndRotation(10.0F, 20.0F, 30.0F", emitted)
        self.assertIn('addOrReplaceChild("_offset"', emitted)
        self.assertIn("PartPose.offset(0.5F, -1.0F, 2.0F)", emitted)
        self.assertIn("PartPose.offsetAndRotation(4.0F, 5.0F, 6.0F", emitted)

    def test_zero_volume_pivot_is_not_rendered_but_textured_planes_are_retained(self) -> None:
        pivot = CONVERTER.Part("pivot", boxes=[
            (0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            (-1.0, 0.0, -2.0, 2.0, 0.0, 4.0),
        ])
        model = CONVERTER.Model("Synthetic", 64, 32, {"pivot": pivot}, set())
        lines: list[str] = []
        CONVERTER.emit_part(lines, model, pivot, "root", "")
        emitted = "\n".join(lines)
        self.assertNotIn("addBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)", emitted)
        self.assertIn("addBox(-1.0F, 0.0F, -2.0F, 2.0F, 0.0F, 4.0F)", emitted)

    def test_source_named_pet_limbs_all_receive_gait_paths(self) -> None:
        dog_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs"
        for name in ("ModelChihuahua.java", "ModelPomeranian.java", "ModelPug.java"):
            path = dog_root / name
            model = CONVERTER.parse_model(path)
            _, phase_a, phase_b, *_ = CONVERTER.animation_profile(model, path)
            self.assertEqual(4, len(phase_a) + len(phase_b), name)

    def test_all_pet_models_map_a_complete_sleeping_pose(self) -> None:
        model_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models"
        paths = sorted([*model_root.glob("cats/Model*.java"), *model_root.glob("dogs/Model*.java")])
        self.assertEqual(22, len(paths))
        for path in paths:
            model = CONVERTER.parse_model(path)
            pose = CONVERTER.sleeping_pose(ROOT, path)
            rendered = CONVERTER.full_pose_java(model, pose)
            common = set(pose.parts) & set(model.parts)
            self.assertGreater(len(common), 10, path.name)
            self.assertEqual(len(common) * 2, rendered.count("new LegacyPartPose("), path.name)

    def test_all_dog_models_have_explicit_sitting_pose_overrides(self) -> None:
        dog_root = ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs"
        models = [CONVERTER.parse_model(path) for path in sorted(dog_root.glob("Model*.java"))]
        self.assertEqual(15, len(models))
        self.assertTrue(all(model.sitting_pose for model in models),
                        [model.name for model in models if not model.sitting_pose])

    def test_collie_sitting_pose_matches_legacy_branch(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/client/models/dogs/ModelCollie.java"
        )
        self.assertEqual((0.0, 12.0, -5.0), model.sitting_pose["body"].pos)
        self.assertAlmostEqual(-0.10049954898833749, model.sitting_pose["body"].rot[0])
        self.assertAlmostEqual(-0.68513423385813, model.sitting_pose["lower_body"].rot[0])
        self.assertEqual(9, len(model.sitting_pose))

    def test_every_catsdogs_entity_has_a_renderer_translation(self) -> None:
        translations = CONVERTER.catsdogs_translations(ROOT)
        mapping = CONVERTER.mappings(ROOT, "catsdogs")
        self.assertEqual(set(mapping), set(translations))
        self.assertEqual(69, len(translations))

    def test_dog_renderer_translations_match_legacy_factories(self) -> None:
        translations = CONVERTER.catsdogs_translations(ROOT)
        for role in ("male", "female", "puppy"):
            self.assertEqual((0.0, -0.1, 0.0), translations[f"{role}_blood_hound"])
            self.assertEqual((0.0, -0.05, 0.0), translations[f"{role}_corgi"])
            self.assertEqual((0.0, -0.1, 0.0), translations[f"{role}_great_dane"])
            self.assertEqual((0.0, 0.1, -0.5), translations[f"{role}_chihuahua"])
            self.assertEqual((0.0, 0.0, 0.0), translations[f"{role}_collie"])
            self.assertEqual((0.0, 0.1, 0.0), translations[f"{role}_fox"])
        self.assertEqual((0.0, 0.0, -0.5), translations["male_pomeranian"])
        self.assertEqual((0.0, 0.0, -0.25), translations["puppy_pomeranian"])
        self.assertEqual((0.0, 0.0, -0.5), translations["male_pug"])
        self.assertEqual((0.0, 0.0, -0.25), translations["puppy_pug"])

    def test_fox_role_scales_match_dedicated_legacy_renderer(self) -> None:
        mapping = CONVERTER.mappings(ROOT, "catsdogs")
        self.assertEqual(1.0, mapping["male_fox"][1])
        self.assertEqual(0.9, mapping["female_fox"][1])
        self.assertEqual(0.5, mapping["puppy_fox"][1])


if __name__ == "__main__":
    unittest.main()
