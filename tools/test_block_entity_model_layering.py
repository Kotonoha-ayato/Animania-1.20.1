"""Regression checks for blocks whose visible geometry is a block-entity renderer."""
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODELS = {
    "base/src/main/resources/assets/animania/models/block/salt_lick.json": "animania:block/salt_lick",
    "extra/src/main/resources/assets/animania_extra/models/block/hamster_wheel.json": "animania_extra:block/hamster_wheel",
    "farm/src/main/resources/assets/animania_farm/models/block/hive.json": "animania_farm:block/hive",
    "farm/src/main/resources/assets/animania_farm/models/block/wild_hive.json": "animania_farm:block/wild_hive",
    "farm/src/main/resources/assets/animania_farm/models/block/block_hive.json": "animania_farm:block/bee_hive",
    "farm/src/main/resources/assets/animania_farm/models/block/block_wild_hive.json": "animania_farm:block/wild_hive",
    **{
        f"catsdogs/src/main/resources/assets/animania_catsdogs/models/block/{name}.json":
        f"animania_catsdogs:block/{name}"
        for name in ("cat_bed_1", "cat_bed_2", "cat_tower", "dog_house", "dog_pillow", "litter_box", "pet_bowl")
    },
}


class BlockEntityModelLayeringTest(unittest.TestCase):
    def test_block_entity_models_only_supply_particles(self) -> None:
        for relative, particle in MODELS.items():
            with self.subTest(model=relative):
                model = json.loads((ROOT / relative).read_text(encoding="utf-8"))
                self.assertEqual({"textures": {"particle": particle}}, model)

    def test_live_block_entity_blocks_hide_vanilla_block_geometry(self) -> None:
        blocks = [
            ROOT / "farm/src/main/java/com/animania/farm/FarmHiveBlock.java",
            ROOT / "extra/src/main/java/com/animania/extra/ExtraHamsterWheelBlock.java",
            ROOT / "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetFacilityBlock.java",
            ROOT / "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlock.java",
            ROOT / "base/src/main/java/com/animania/common/block/AnimaniaSaltLickBlock.java",
        ]
        for block in blocks:
            with self.subTest(block=block):
                self.assertIn("return RenderShape.INVISIBLE", block.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
