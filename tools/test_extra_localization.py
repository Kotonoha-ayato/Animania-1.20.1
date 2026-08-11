import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class ExtraLocalizationTest(unittest.TestCase):
    def test_chinese_dart_frog_egg_alias_is_localized(self) -> None:
        path = ROOT / "extra/src/main/resources/assets/animania_extra/lang/zh_cn.json"
        language = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual("箭毒蛙", language["item.animania_extra.entity_egg_dart_frog"])
        self.assertEqual("箭毒蛙", language["item.animania_extra.entity_egg_dartfrog"])


if __name__ == "__main__":
    unittest.main()
