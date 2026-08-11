"""Validate the migration matrix/evidence documents against their JSON schemas."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from jsonschema import Draft202012Validator


def validate(document: Path, schema: Path) -> list[str]:
    value = json.loads(document.read_text(encoding="utf-8-sig"))
    schema_value = json.loads(schema.read_text(encoding="utf-8-sig"))
    return [error.message for error in Draft202012Validator(schema_value).iter_errors(value)]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    args = parser.parse_args()
    root = args.root.resolve()
    documents = [(root / "docs/migration-matrix.json", root / "docs/migration-matrix.schema.json")]
    errors = []
    for document, schema in documents:
        errors.extend(f"{document}: {message}" for message in validate(document, schema))
    print(json.dumps({"documents": [str(item[0]) for item in documents],
                      "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
