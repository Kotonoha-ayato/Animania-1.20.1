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

    def test_root_coordinates_match_pinned_craftstudio_reader(self) -> None:
        lines = []
        CONVERTER.emit_node(lines, {
            "name": "RootPart", "size": [2, 4, 6], "offsetFromPivot": [1, 2, 3],
            "position": [4, 5, 6], "rotation": [0, 0, 0], "texOffset": [0, 0],
        }, "root", set())
        self.assertIn("addBox(0.0F, -4.0F, -6.0F, 2.0F, 4.0F, 6.0F)", lines[0])
        self.assertIn("PartPose.offsetAndRotation(4.0F, 19.0F, -6.0F", lines[0])

    def test_child_coordinates_match_pinned_craftstudio_reader(self) -> None:
        lines = []
        CONVERTER.emit_node(lines, {
            "name": "Child", "size": [2, 2, 2], "offsetFromPivot": [0, 0, 0],
            "position": [4, 5, 6], "rotation": [10, 20, 30], "texOffset": [0, 0],
        }, "parent_part", set())
        self.assertIn("PartPose.offsetAndRotation(4.0F, -5.0F, -6.0F", lines[0])
        raw = tuple(math.radians(value) for value in (10, -20, -30))
        converted = CONVERTER.legacy_euler_to_modelpart(*raw)
        for value in converted:
            self.assertIn(CONVERTER.fl(value), lines[0])

    def test_craftstudio_multi_axis_rotation_uses_quaternion_order(self) -> None:
        raw = tuple(math.radians(value) for value in (20, 30, 40))
        converted = CONVERTER.legacy_euler_to_modelpart(*raw)
        self.assertNotEqual(tuple(round(value, 6) for value in raw),
                            tuple(round(value, 6) for value in converted))

    def test_plain_runtime_cube_preserves_negative_y_and_z_extents(self) -> None:
        vertices = CONVERTER.legacy_vertices({
            "size": [2, 4, 6], "offsetFromPivot": [1, 2, 3]
        })
        self.assertEqual(vertices[0], [0.0, 0.0, 0.0])
        self.assertEqual(vertices[6], [2.0, -4.0, -6.0])

    def test_custom_vertices_use_reader_corner_permutation_and_axis_flips(self) -> None:
        source = [[float(index), float(index + 10), float(index + 20)] for index in range(8)]
        vertices = CONVERTER.legacy_vertices({
            "size": [8, 8, 8], "offsetFromPivot": [1, 2, 3], "vertexCoords": source
        })
        self.assertEqual(vertices[0], [4.0, -15.0, -26.0])  # exported corner 3
        self.assertEqual(vertices[7], [6.0, -17.0, -28.0])  # exported corner 5

    def test_legacy_uv_rectangles_are_not_vanilla_cube_uvs(self) -> None:
        rectangles = CONVERTER.legacy_texture_rects({"size": [2, 4, 6], "texOffset": [10, 20]})
        self.assertEqual(rectangles[0], [24, 30, 18, 26])
        self.assertEqual(rectangles[5], [18, 30, 16, 26])

    def test_parent_offset_is_inherited_by_child_pivot(self) -> None:
        lines = []
        CONVERTER.emit_runtime_node(lines, {
            "name": "Parent", "size": [1, 1, 1], "offsetFromPivot": [1, 2, 3],
            "position": [0, 0, 0], "rotation": [0, 0, 0], "texOffset": [0, 0],
            "children": [{
                "name": "Child", "size": [1, 1, 1], "offsetFromPivot": [0, 0, 0],
                "position": [4, 5, 6], "rotation": [0, 0, 0], "texOffset": [0, 0]
            }]
        }, True, 64, 32, set())
        child_line = next(line for line in lines if "PartPose.offsetAndRotation(5.0F, -7.0F, -9.0F" in line)
        self.assertIn("LegacyCraftStudioModel.part", child_line)


if __name__ == "__main__":
    unittest.main()
