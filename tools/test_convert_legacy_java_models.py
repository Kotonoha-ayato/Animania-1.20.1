"""Regression checks for the native 1.12 Java-model converter."""

from __future__ import annotations

import importlib.util
import sys
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


class LegacyJavaModelConverterTest(unittest.TestCase):
    def test_commented_angus_horns_are_not_geometry(self) -> None:
        model = CONVERTER.parse_model(
            ROOT / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/client/model/cow/ModelCowAngus.java"
        )
        self.assertNotIn("Horn1", model.parts)
        self.assertNotIn("Horn2", model.parts)

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

    def test_offset_stays_after_rotation_and_is_inherited_by_children(self) -> None:
        parent = CONVERTER.Part("parent", boxes=[(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)],
                                pos=(10.0, 20.0, 30.0), offset=(0.5, -1.0, 2.0), children=["child"])
        child = CONVERTER.Part("child", boxes=[(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)],
                               pos=(4.0, 5.0, 6.0), offset=(-0.5, 1.0, -2.0))
        model = CONVERTER.Model("Synthetic", 64, 32, {"parent": parent, "child": child}, set())
        lines: list[str] = []
        CONVERTER.emit_part(lines, model, parent, "root", "")
        emitted = "\n".join(lines)
        self.assertIn("addBox(1.5F, 1.0F, 5.0F, 4.0F, 5.0F, 6.0F)", emitted)
        self.assertIn("PartPose.offsetAndRotation(10.0F, 20.0F, 30.0F", emitted)
        self.assertIn("PartPose.offsetAndRotation(4.5F, 4.0F, 8.0F", emitted)

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
