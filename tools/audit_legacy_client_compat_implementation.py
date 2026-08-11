"""Individually map legacy client and optional-compat entry points.

The audit has two deliberately separate branches: old JEI/Waila/TOP providers
map to the optional JEI/Jade/TOP plugins, while old Base client classes map to
the native client/renderer/network entry points.  It proves target ownership
and compiled contract tests only.  Actual client launch, atlas capture and
optional-mod runtime callbacks remain unproven by design.
"""
from __future__ import annotations

import argparse
import glob
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


COMPAT_TARGETS = [
    "base/src/main/java/com/animania/compat/jei/AnimaniaJeiPlugin.java",
    "base/src/main/java/com/animania/compat/jade/AnimaniaJadePlugin.java",
    "base/src/main/java/com/animania/compat/top/AnimaniaTopProbeCompat.java",
    "base/src/main/java/com/animania/compat/AnimaniaProbeComponents.java",
]
COMPAT_TEST = "base/src/test/java/com/animania/compat/AnimaniaCompatContractTest.java"
COMPAT_XML = "base/build/test-results/test/TEST-com.animania.compat.AnimaniaCompatContractTest.xml"
COMPAT_SELECTOR = "optionalIntegrationsUseModernRegistrationEntrypoints()"
CLIENT_TARGETS = [
    "base/src/main/java/com/animania/client/AnimaniaClient.java",
    "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java",
    "base/src/main/java/com/animania/client/render/LegacyAnimalTextures.java",
    "base/src/main/java/com/animania/client/render/AnimaniaEggItemRenderer.java",
    "base/src/main/java/com/animania/client/render/AnimaniaCarryRenderer.java",
    "base/src/main/java/com/animania/client/config/AnimaniaConfigScreen.java",
]
CLIENT_TEST = "base/src/test/java/com/animania/client/BaseClientContractTest.java"
CLIENT_XML = "base/build/test-results/test/TEST-com.animania.client.BaseClientContractTest.xml"
CLIENT_SELECTOR = "allFacilityRenderersUseNativeModelPartsAndNoCraftStudioRuntime()"
NETWORK_TARGETS = [
    "base/src/main/java/com/animania/network/AnimaniaNetwork.java",
    "base/src/main/java/com/animania/network/RequestAnimalSnapshotPacket.java",
]
NETWORK_TEST = "base/src/test/java/com/animania/network/AnimaniaNetworkContractTest.java"
NETWORK_XML = "base/build/test-results/test/TEST-com.animania.network.AnimaniaNetworkContractTest.xml"
NETWORK_SELECTOR = "onlyValidatedSnapshotRequestsCrossTheSimpleChannel()"


def selector_passes(report: Path, selector: str) -> bool:
    try:
        suite = ET.parse(report).getroot()
    except (OSError, ET.ParseError):
        return False
    return any(case.attrib.get("name") == selector
               and not (case.findall("failure") or case.findall("error") or case.findall("skipped"))
               for case in suite.findall(".//testcase"))


def owned_implementation(evidence_dir: Path) -> set[str]:
    owned: set[str] = set()
    for filename in glob.glob(str(evidence_dir / "*.json")):
        try:
            data = read_json(Path(filename))
        except (OSError, json.JSONDecodeError):
            continue
        if data.get("audit_id") == "legacy-client-compat-implementation":
            continue
        results = data.get("results", [])
        if isinstance(results, list):
            owned.update(str(row.get("entry_id")) for row in results
                         if row.get("requirement_id") == "implementation" and row.get("result") == "pass")
    return owned


def branch(entry: dict) -> tuple[str, list[str], str, str, str, str] | None:
    source = str(entry.get("source", "")).replace("\\", "/")
    if "/compat/" in source:
        return ("optional JEI/Jade/TOP compatibility", COMPAT_TARGETS, COMPAT_TEST, COMPAT_XML, COMPAT_SELECTOR,
                "modern optional-plugin registration")
    if entry.get("module") == "base" and "/network/client/" in source:
        return ("client packet replacement", NETWORK_TARGETS, NETWORK_TEST, NETWORK_XML, NETWORK_SELECTOR,
                "SimpleChannel server-authoritative packet path")
    if entry.get("module") == "base" and "/client/" in source:
        return ("native Base client conversion", CLIENT_TARGETS, CLIENT_TEST, CLIENT_XML, CLIENT_SELECTOR,
                "native client ModelPart/renderer entry path")
    return None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = read_json(matrix_path)
    auditor_path = "tools/audit_legacy_client_compat_implementation.py"
    owned = owned_implementation(evidence_dir)
    results, rows, errors = [], [], []
    contracts = [(COMPAT_TARGETS, COMPAT_TEST, COMPAT_XML, COMPAT_SELECTOR),
                 (CLIENT_TARGETS, CLIENT_TEST, CLIENT_XML, CLIENT_SELECTOR),
                 (NETWORK_TARGETS, NETWORK_TEST, NETWORK_XML, NETWORK_SELECTOR)]
    for targets, test, xml, selector in contracts:
        if not all((root / path).is_file() for path in [*targets, test, xml]) or not selector_passes(root / xml, selector):
            errors.append(f"missing target or selected passing contract: {selector}")
    for entry in matrix.get("entries", []):
        if entry.get("kind") != "java" or entry.get("status") == "closed" or entry.get("entry_id") in owned:
            continue
        if "implementation" not in entry.get("requirements", []):
            continue
        selected = branch(entry)
        if selected is None:
            continue
        family, targets, test, xml, selector, guard = selected
        source = str(entry["source"]).replace("\\", "/")
        old = root / "upstream/Animania-1.12" / source
        target_files = [root / path for path in targets]
        if not old.is_file() or not all(path.is_file() for path in target_files):
            errors.append(f"missing source/target mapping for {source}")
            continue
        merged = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in target_files)
        if family.startswith("optional"):
            valid = all(token in merged for token in ("@JeiPlugin", "@WailaPlugin", "getTheOneProbe"))
        elif family.startswith("client packet"):
            valid = "SimpleChannel" in merged and "registerMessage" in merged
        else:
            valid = "registerBlockEntityRenderer" in merged and "LegacyAnimalModel" in merged
        if not valid:
            errors.append(f"target implementation guard failed for {source}")
            continue
        proof = evidence_dir / "legacy-client-compat-implementation" / entry["entry_id"] / "proof.json"
        write_json(proof, {
            "entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
            "legacy_classes": entry.get("classes", []), "family": family, "modern_targets": targets,
            "guard": guard, "test_selector": selector,
        })
        results.append({
            "entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": ([{"path": path, "sha256": sha256(root / path)} for path in targets]
                             + [{"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}]),
            "tests": [{"selector": f"{xml}::{selector}", "result": "pass", "artifact": xml,
                       "artifact_sha256": sha256(root / xml)}],
            "evidence_kind": "source_mapping", "test_code_path": test, "test_code_sha256": sha256(root / test),
            "notes": [
                f"[legacy-client-compat-implementation-v1] {Path(source).stem} maps to {family}: {guard}. "
                "The current contract selector passed, but this is implementation-only evidence; real optional-mod startup, client atlas initialization, and visual regression requirements remain open."
            ],
        })
        rows.append({"entry_id": entry["entry_id"], "source": source, "family": family, "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "legacy-client-compat-implementation-v1-report.json", {
        "schema_version": 1, "audit": "legacy-client-compat-implementation", "audit_version": "v1",
        "rows": rows, "errors": errors, "error_count": len(errors),
    })
    write_json(evidence_dir / "legacy-client-compat-implementation-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "legacy-client-compat-implementation", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_legacy_client_compat_implementation.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path), "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
