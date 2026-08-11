from __future__ import annotations

import unittest

from tools.ensure_locales import catsdogs_zh_cn_overrides


class EnsureLocalesTest(unittest.TestCase):
    def test_catsdogs_chinese_entity_and_egg_names(self) -> None:
        keys = {
            "entity.animania_catsdogs.female_labrador",
            "entity.animania_catsdogs.puppy_great_dane",
            "entity.animania_catsdogs.tom_ragdoll",
            "item.animania_catsdogs.entity_egg_female_labrador",
            "item.animania_catsdogs.entity_egg_kitten_siamese",
        }
        values = catsdogs_zh_cn_overrides(keys)
        self.assertEqual("拉布拉多犬（母）", values["entity.animania_catsdogs.female_labrador"])
        self.assertEqual("大丹犬（幼犬）", values["entity.animania_catsdogs.puppy_great_dane"])
        self.assertEqual("布偶猫（公猫）", values["entity.animania_catsdogs.tom_ragdoll"])
        self.assertEqual("拉布拉多犬（母）刷怪蛋", values["item.animania_catsdogs.entity_egg_female_labrador"])
        self.assertEqual("暹罗猫（幼猫）刷怪蛋", values["item.animania_catsdogs.entity_egg_kitten_siamese"])


if __name__ == "__main__":
    unittest.main()
