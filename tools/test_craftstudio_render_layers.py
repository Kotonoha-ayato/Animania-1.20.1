"""Regression checks for render layers used by exact CraftStudio prop meshes."""
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class CraftStudioRenderLayerTest(unittest.TestCase):
    def test_solid_facilities_restore_legacy_culling(self) -> None:
        renderers = [
            ROOT / "catsdogs/src/main/java/com/animania/catsdogs/client/render/CatsDogsPetFacilityRenderer.java",
            ROOT / "farm/src/main/java/com/animania/farm/client/render/FarmHiveRenderer.java",
            ROOT / "farm/src/main/java/com/animania/farm/client/render/FarmHiveItemRenderer.java",
        ]
        for renderer in renderers:
            source = renderer.read_text(encoding="utf-8")
            self.assertIn("RenderType.entityCutout(", source, renderer)
            self.assertNotIn("entityCutoutNoCull", source, renderer)

    def test_zero_thickness_hamster_wheel_remains_deliberately_two_sided(self) -> None:
        renderer = ROOT / "extra/src/main/java/com/animania/extra/client/render/ExtraHamsterWheelRenderer.java"
        self.assertIn("RenderType.entityCutoutNoCull(WHEEL_TEXTURE)", renderer.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
