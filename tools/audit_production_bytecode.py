"""Fail if a staged Forge release JAR still contains mapped development symbols."""
from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path


CLASS_SENTINELS = {
    "com/animania/common/advancement/FeedAnimalTrigger.class": (b"register",),
    "com/animania/common/entity/AnimaniaVehicleEntity.class": (b"OPTIONAL_UUID", b"defineId"),
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args()
    errors: list[str] = []
    try:
        with zipfile.ZipFile(args.jar) as archive:
            names = set(archive.namelist())
            if "META-INF/mods.toml" not in names:
                errors.append("missing META-INF/mods.toml")
            for class_name, forbidden in CLASS_SENTINELS.items():
                if class_name not in names:
                    continue
                bytecode = archive.read(class_name)
                for symbol in forbidden:
                    if symbol in bytecode:
                        errors.append(f"{class_name} retains mapped symbol {symbol.decode()}")
    except (OSError, zipfile.BadZipFile) as exc:
        errors.append(f"invalid release JAR: {exc}")
    if errors:
        for error in errors:
            print(f"production-bytecode-audit: {error}", file=sys.stderr)
        raise SystemExit(1)
    print(f"production-bytecode-audit: PASS {args.jar}")


if __name__ == "__main__":
    main()
