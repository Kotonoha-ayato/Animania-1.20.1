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


if __name__ == "__main__":
    unittest.main()
