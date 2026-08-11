"""Run the Java 17 public API contract audit without editing the matrix.

The migration matrix can only be closed by the central evidence writer.  This
report is a separate stage-6 gate: it binds the required gameplay state
methods to the actual JUnit XML produced by the Base test task and records
the source/test fingerprints used for the decision.
"""
from __future__ import annotations

import argparse
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import sha256, write_json


REQUIRED_METHODS = (
    "typeId", "getGender", "setGender", "age", "isAdult", "getVariantName",
    "setVariantName", "getHunger", "getThirst", "feed", "drink", "isSleeping",
    "setSleeping", "isPlaying", "setPlaying", "play", "canBreedWith",
    "isPregnant", "setPregnant", "pregnancyTicks", "gestationTicks",
    "isSterilized", "setSterilized", "mateUuid", "setMateUuid", "parentUuid",
    "setParentUuid", "snapshot", "asMob", "isTamed", "isSitting", "isSaddled",
    "isMilkReady", "isInBall",
)
REQUIRED_FACADE_METHODS = (
    "registerSpecies", "species", "speciesOf", "speciesIds", "speciesForAddon",
    "hasSpecies", "registerTamingRequirement", "requiresTaming",
    "registerFoodMatcher", "matchesRegisteredFood", "isAddonLoaded",
)
FACADE_SOURCES = (
    "base/src/main/java/com/animania/api/IAnimaniaAnimal.java",
    "base/src/main/java/com/animania/api/AnimaniaApi.java",
)
TEST_REPORTS = (
    "base/build/test-results/test/TEST-com.animania.api.PublicApiContractTest.xml",
    "base/build/test-results/test/TEST-com.animania.api.AnimaniaApiTest.xml",
)


def junit_result(root: Path, relative: str) -> dict:
    path = root / relative
    if not path.is_file():
        return {"path": relative, "exists": False, "tests": 0, "failures": 0,
                "errors": 0, "skipped": 0, "sha256": None, "selectors": []}
    try:
        suite = ET.parse(path).getroot()
        tests = int(suite.attrib.get("tests", "0"))
        failures = int(suite.attrib.get("failures", "0"))
        errors = int(suite.attrib.get("errors", "0"))
        skipped = int(suite.attrib.get("skipped", "0"))
        selectors = [case.attrib.get("name", "") for case in suite.findall(".//testcase")]
        return {"path": relative, "exists": True, "tests": tests,
                "failures": failures, "errors": errors, "skipped": skipped,
                "sha256": sha256(path), "selectors": selectors}
    except (OSError, ET.ParseError, ValueError) as exc:
        return {"path": relative, "exists": False, "tests": 0, "failures": 0,
                "errors": 1, "skipped": 0, "sha256": None, "selectors": [],
                "parse_error": str(exc)}


def contains_methods(text: str, methods: tuple[str, ...]) -> list[str]:
    return [method for method in methods if not re.search(r"\b" + re.escape(method) + r"\s*\(", text)]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("build/api-contract-audit.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output

    api_text = (root / FACADE_SOURCES[0]).read_text(encoding="utf-8") if (root / FACADE_SOURCES[0]).is_file() else ""
    facade_text = (root / FACADE_SOURCES[1]).read_text(encoding="utf-8") if (root / FACADE_SOURCES[1]).is_file() else ""
    missing_animal_methods = contains_methods(api_text, REQUIRED_METHODS)
    missing_facade_methods = contains_methods(facade_text, REQUIRED_FACADE_METHODS)
    source_fingerprints = []
    for relative in FACADE_SOURCES:
        path = root / relative
        if path.is_file():
            source_fingerprints.append({"path": relative, "sha256": sha256(path)})

    tests = [junit_result(root, relative) for relative in TEST_REPORTS]
    errors = []
    if missing_animal_methods:
        errors.append("missing animal contract methods: " + ", ".join(missing_animal_methods))
    if missing_facade_methods:
        errors.append("missing addon facade methods: " + ", ".join(missing_facade_methods))
    if len(source_fingerprints) != len(FACADE_SOURCES):
        errors.append("one or more public API source files are missing")
    for test in tests:
        if not test["exists"]:
            errors.append("missing or unreadable JUnit report: " + test["path"])
        elif test["tests"] <= 0 or test["failures"] or test["errors"] or test["skipped"]:
            errors.append("non-green JUnit report: " + test["path"])

    report = {
        "schema_version": 1,
        "audit": "api-contract",
        "audit_version": "v1",
        "required_animal_methods": list(REQUIRED_METHODS),
        "required_addon_facade_methods": list(REQUIRED_FACADE_METHODS),
        "missing_animal_methods": missing_animal_methods,
        "missing_addon_facade_methods": missing_facade_methods,
        "source_fingerprints": source_fingerprints,
        "tests": tests,
        "errors": errors,
        "error_count": len(errors),
        "all_passed": not errors,
    }
    write_json(output, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
