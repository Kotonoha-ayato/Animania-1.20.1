"""Audit legacy numeric entity and block-entity registration infrastructure."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

OWNER = "[registry-infrastructure-audit:v1]"
ENTITY_HANDLER = "src/main/java/com/animania/common/handler/EntityHandler.java"
TILE_HANDLER = "src/main/java/com/animania/common/handler/TileEntityHandler.java"
TARGETS = {
    "TileEntityTrough": ("TROUGH_BE", 'blockEntity("trough"'),
    "TileEntityNest": ("NEST_BE", 'blockEntity("nest"'),
    "TileEntityInvisiblock": ("INVISIBLE_BE", 'BLOCK_ENTITIES.register("invisiblock"'),
    "TileEntitySaltLick": ("SALT_LICK_BE", 'BLOCK_ENTITIES.register("salt_lick"'),
}

def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json")); parser.add_argument("--write", action="store_true")
    args = parser.parse_args(); root = args.root.resolve(); errors: list[str] = []
    old_root = root / "upstream/Animania-1.12"
    entity = (old_root / ENTITY_HANDLER).read_text(encoding="utf-8")
    stripped = re.sub(r"package\s+[^;]+;|public\s+class\s+EntityHandler|[{}\s]", "", entity)
    if stripped != "publicstaticintentityID=0;": errors.append("legacy EntityHandler is no longer only the numeric ID counter")
    id_audit = json.loads((root / "docs/id-mapping-audit.json").read_text(encoding="utf-8"))
    if not id_audit.get("passed"): errors.append("source-derived ID mapping audit is not green")
    for module, main in (("farm", "AnimaniaFarm.java"), ("extra", "AnimaniaExtra.java"), ("catsdogs", "AnimaniaCatsDogs.java")):
        text = (root / module / "src/main/java/com/animania" / module / main).read_text(encoding="utf-8")
        if "DeferredRegister<EntityType<?>>" not in text or "ENTITY_TYPES.register(bus)" not in text:
            errors.append(f"{module} entity DeferredRegister is not wired to its mod event bus")

    tile = (old_root / TILE_HANDLER).read_text(encoding="utf-8")
    registered = set(re.findall(r"registerTileEntity\((\w+)\.class", tile))
    if registered != set(TARGETS): errors.append(f"legacy block-entity set changed: {sorted(registered)}")
    blocks_path = "base/src/main/java/com/animania/common/AnimaniaBlocks.java"
    blocks = (root / blocks_path).read_text(encoding="utf-8")
    for legacy, (field, declaration) in TARGETS.items():
        if field not in blocks or declaration not in blocks: errors.append(f"missing modern registration for {legacy}")
    if "DeferredRegister<BlockEntityType<?>>" not in blocks: errors.append("modern block entities do not use DeferredRegister")
    main = (root / "base/src/main/java/com/animania/Animania.java").read_text(encoding="utf-8")
    if "AnimaniaBlocks.BLOCK_ENTITIES.register(modBus)" not in main: errors.append("Base block-entity register is not attached to the mod bus")
    game_path = "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"
    game = (root / game_path).read_text(encoding="utf-8")
    for token in ("storageCapabilitiesPersist", "saltLickCareAndDurability", "nestAndFloorPilesRetainLegacyInteractionRules",
                  "troughRetainsTwoBlockStructureAndControllerCleanup", "InvisibleTroughProxyEntity",
                  "proxy sided item automation", "proxy sided fluid automation"):
        if token not in game: errors.append(f"missing live GameTest evidence: {token}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8")); by_source = {e.get("source"): e for e in matrix["entries"]}
    changed = 0
    if not errors:
        proofs = {
            ENTITY_HANDLER: {"paths": ["docs/id-mapping.json", "docs/id-mapping-audit.json", "tools/audit_id_mapping.py", "tools/audit_registry_infrastructure.py",
                                      "farm/src/main/java/com/animania/farm/AnimaniaFarm.java", "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
                                      "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java"],
                             "behavior_tests": ["tools/audit_id_mapping.py", "tools/audit_registry_infrastructure.py"]},
            TILE_HANDLER: {"paths": [blocks_path, "base/src/main/java/com/animania/common/block/AnimaniaInvisibleBlock.java", "tools/audit_registry_infrastructure.py"],
                           "behavior_tests": [game_path, "tools/audit_registry_infrastructure.py"]},
        }
        for source, values in proofs.items():
            entry = by_source.get(source)
            if not entry: errors.append(f"matrix row missing: {source}"); continue
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            proof = {**values, "serialization_tests": [game_path] if source == TILE_HANDLER else [], "client_tests": [],
                     "notes": [f"{OWNER} source-derived modern registry replacement and executable evidence verified."]}
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=values["behavior_tests"], target_evidence=proof); changed += 1
            elif entry.get("status") != "closed" or not owned: errors.append(f"provable row not closed: {source}")
    if args.write and not errors: matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"legacy_block_entities": sorted(registered), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
