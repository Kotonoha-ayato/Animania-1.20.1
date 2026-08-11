import importlib.util
import math
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "convert_craftstudio_models", ROOT / "tools" / "convert_craftstudio_models.py")
CONVERTER = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(CONVERTER)


class CraftStudioConversionTest(unittest.TestCase):
    def test_zero_thickness_spoke_is_preserved(self) -> None:
        lines = []
        CONVERTER.emit_node(lines, {
            "name": "Stick", "size": [1, 13, 0], "offsetFromPivot": [0, 0, 0],
            "position": [0, 0, 0], "rotation": [0, 0, 0], "texOffset": [0, 0],
        }, "root", set())
        self.assertIn("addBox(-0.5F, -6.5F, 0.0F, 1.0F, 13.0F, 0.0F)", lines[0])

    def test_craftstudio_multi_axis_rotation_uses_quaternion_order(self) -> None:
        raw = tuple(math.radians(value) for value in (20, 30, 40))
        converted = CONVERTER.legacy_euler_to_modelpart(*raw)
        self.assertNotEqual(tuple(round(value, 6) for value in raw),
                            tuple(round(value, 6) for value in converted))


if __name__ == "__main__":
    unittest.main()
