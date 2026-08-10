"""Generate immutable legacy-ID lists used by the four modern registries."""
from __future__ import annotations

import argparse
import re
from pathlib import Path

MODULES = ("farm", "extra", "catsdogs")
PATTERN = re.compile(r'register(?:Animal|Entity)\s*\([^;]*?,\s*"([a-z0-9_]+)"', re.I | re.S)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    java_root = args.source / "src" / "main" / "java" / "com" / "animania" / "addons"
    for module in MODULES:
        ids = set()
        for source in (java_root / module).rglob("*.java"):
            ids.update(match.lower() for match in PATTERN.findall(source.read_text(encoding="utf-8", errors="replace")))
        package = f"com.animania.{module}"
        type_name = "Farm" if module == "farm" else "Extra" if module == "extra" else "CatsDogs"
        target = args.output / module / "src" / "main" / "java" / "com" / "animania" / module / f"{type_name}LegacyIds.java"
        target.parent.mkdir(parents=True, exist_ok=True)
        values = ",\n".join(f'            "{item}"' for item in sorted(ids))
        target.write_text(
            f"package {package};\n\nimport java.util.List;\n\n/** IDs extracted from the pinned 1.12 registration calls. */\n"
            f"public final class {type_name}LegacyIds {{\n    public static final List<String> ALL = List.of(\n{values}\n    );\n\n"
            f"    private {type_name}LegacyIds() {{}}\n}}\n",
            encoding="utf-8",
        )
        print(f"{module}: {len(ids)} ids -> {target}")


if __name__ == "__main__":
    main()

