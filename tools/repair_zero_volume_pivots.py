#!/usr/bin/env python3
"""Remove renderable 0×0×0 cubes from converted legacy ModelPart layers.

Minecraft 1.12 used 0×0×0 ModelRenderer boxes as transform-only pivots.  A
modern ModelPart attempts to render those degenerate boxes, which appears as a
black pixel on some render paths.  This tool deliberately changes only boxes
whose width, height, and depth are all exactly zero; it leaves 0-thickness
planes (for feathers, fur and similar details) intact.
"""

from __future__ import annotations

import argparse
import pathlib
import re


ZERO_VOLUME_BOX = re.compile(
    r"CubeListBuilder\.create\(\)\.texOffs\([^)]*\)\.addBox\("
    r"[^,]+,\s*[^,]+,\s*[^,]+,\s*0\.0F,\s*0\.0F,\s*0\.0F\)"
)


def repair(path: pathlib.Path) -> int:
    original = path.read_text(encoding="utf-8")
    repaired, count = ZERO_VOLUME_BOX.subn("CubeListBuilder.create()", original)
    if count:
        path.write_text(repaired, encoding="utf-8", newline="\n")
    return count


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=pathlib.Path, default=pathlib.Path("."))
    args = parser.parse_args()
    root = args.root.resolve()
    total = 0
    for path in sorted(root.glob("*/src/main/java/**/**LegacyModelLayers.java")):
        count = repair(path)
        if count:
            print(f"{path.relative_to(root)}: removed {count} zero-volume pivot cubes")
            total += count
    print(f"removed {total} zero-volume pivot cubes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
