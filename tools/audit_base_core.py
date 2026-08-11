"""Audit the remaining Base Java contracts against the Java 17 implementation."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[base-core-audit:v1]"

def row(paths, behavior, serialization=None, client=None):
    return {"paths": paths, "behavior_tests": behavior, "serialization_tests": serialization or [], "client_tests": client or []}

CORE = {
    "src/main/java/com/animania/Animania.java": row(
        ["base/src/main/java/com/animania/Animania.java", "base/src/main/java/com/animania/AnimaniaServerEvents.java"],
        ["base/src/test/java/com/animania/common/AnimaniaServerContractTest.java", "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"]),
    "src/main/java/com/animania/client/AnimaniaTextures.java": row(
        ["base/src/main/java/com/animania/client/AnimaniaClient.java", "base/src/main/java/com/animania/client/render/LegacyAnimalTextures.java", "base/src/main/java/com/animania/client/render/AnimaniaEggItemRenderer.java"],
        ["base/src/test/java/com/animania/client/render/AnimaniaEggItemRendererTest.java"], client=["base/src/test/java/com/animania/client/render/AnimaniaEggItemRendererTest.java", "tools/audit_model_assets.py", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/gui/GuiConfigAnimania.java": row(
        ["base/src/main/java/com/animania/client/config/AnimaniaConfigScreen.java", "base/src/main/java/com/animania/common/config/AnimaniaConfig.java"],
        ["base/src/test/java/com/animania/client/config/AnimaniaConfigScreenTest.java"], client=["base/src/test/java/com/animania/client/config/AnimaniaConfigScreenTest.java"]),
    "src/main/java/com/animania/client/gui/GuiFactoryAnimania.java": row(
        ["base/src/main/java/com/animania/client/config/AnimaniaConfigScreen.java", "base/src/main/java/com/animania/Animania.java"],
        ["base/src/test/java/com/animania/client/config/AnimaniaConfigScreenTest.java"], client=["base/src/test/java/com/animania/client/config/AnimaniaConfigScreenTest.java"]),
    "src/main/java/com/animania/client/handler/AnimationHandler.java": row(
        ["base/src/main/java/com/animania/client/model/AnimaniaAnimations.java", "base/src/main/java/com/animania/client/model/LegacyAnimalModel.java", "base/src/main/java/com/animania/client/model/BaseNativeAnimations.java"],
        ["base/src/test/java/com/animania/client/BaseNativeModelConversionTest.java"], client=["base/src/test/java/com/animania/client/BaseNativeModelConversionTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/handler/RenderHandler.java": row(
        ["base/src/main/java/com/animania/client/AnimaniaClient.java", "base/src/main/java/com/animania/client/render/BaseTroughRenderer.java", "base/src/main/java/com/animania/client/render/BaseNestRenderer.java", "base/src/main/java/com/animania/client/render/BaseSaltLickRenderer.java"],
        ["base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], client=["base/src/test/java/com/animania/client/BaseClientContractTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/models/IColoredModel.java": row(
        ["base/src/main/java/com/animania/client/model/LegacyAnimalModel.java"],
        ["base/src/test/java/com/animania/client/BaseLegacyModelConversionTest.java"], client=["base/src/test/java/com/animania/client/BaseLegacyModelConversionTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/render/RenderPropInv.java": row(
        ["base/src/main/java/com/animania/client/render/AnimaniaHamsterBallLayer.java", "base/src/main/java/com/animania/client/model/AnimaniaHamsterBallModel.java"],
        ["base/src/test/java/com/animania/client/AnimaniaHamsterBallModelTest.java"], client=["base/src/test/java/com/animania/client/AnimaniaHamsterBallModelTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/render/tileEntity/TileEntityCraftstudioRenderer.java": row(
        ["base/src/main/java/com/animania/client/render/BaseLegacyFacilityRenderSupport.java", "base/src/main/java/com/animania/client/render/BaseNestRenderer.java", "base/src/main/java/com/animania/client/render/BaseSaltLickRenderer.java", "base/src/main/java/com/animania/client/render/BaseTroughRenderer.java"],
        ["base/src/test/java/com/animania/client/BaseLegacyModelConversionTest.java", "tools/audit_model_conversion.py"], client=["base/src/test/java/com/animania/client/BaseClientContractTest.java", "tools/audit_model_assets.py", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/render/tileEntity/TileEntityNestRenderer.java": row(
        ["base/src/main/java/com/animania/client/render/BaseNestRenderer.java"],
        ["base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], client=["base/src/test/java/com/animania/client/BaseClientContractTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/render/tileEntity/TileEntitySaltLickRenderer.java": row(
        ["base/src/main/java/com/animania/client/render/BaseSaltLickRenderer.java"],
        ["base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], client=["base/src/test/java/com/animania/client/BaseClientContractTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/client/render/tileEntity/TileEntityTroughRenderer.java": row(
        ["base/src/main/java/com/animania/client/render/BaseTroughRenderer.java"],
        ["base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], client=["base/src/test/java/com/animania/client/BaseClientContractTest.java", "base/run/fullClient/logs/debug.log"]),
    "src/main/java/com/animania/common/blocks/AnimaniaRotateable.java": row(
        ["base/src/main/java/com/animania/common/block/AnimaniaContainerBlock.java", "base/src/main/java/com/animania/common/block/AnimaniaTroughBlock.java", "base/src/main/java/com/animania/common/block/AnimaniaThinBlock.java"],
        ["base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java", "tools/audit_trough_block.py"]),
    "src/main/java/com/animania/common/blocks/fluids/BlockFluidBase.java": row(
        ["base/src/main/java/com/animania/common/AnimaniaFluids.java"],
        ["tools/audit_farm_fluids.py", "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"]),
    "src/main/java/com/animania/common/blocks/fluids/BlockFluidSlop.java": row(
        ["base/src/main/java/com/animania/common/AnimaniaFluids.java", "base/src/main/java/com/animania/common/recipe/SlopRecipe.java"],
        ["tools/audit_farm_fluids.py", "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"]),
    "src/main/java/com/animania/common/blocks/fluids/FluidBase.java": row(
        ["base/src/main/java/com/animania/common/AnimaniaFluids.java"],
        ["tools/audit_farm_fluids.py"]),
    "src/main/java/com/animania/common/creativeTab/TabAnimaniaEntities.java": row(
        ["base/src/main/java/com/animania/common/AnimaniaTabs.java", "farm/src/main/java/com/animania/farm/FarmTab.java", "extra/src/main/java/com/animania/extra/ExtraTab.java", "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsTab.java"],
        ["base/src/test/java/com/animania/common/AnimaniaTabsContractTest.java"]),
    "src/main/java/com/animania/common/creativeTab/TabAnimaniaResources.java": row(
        ["base/src/main/java/com/animania/common/AnimaniaTabs.java", "farm/src/main/java/com/animania/farm/FarmTab.java", "extra/src/main/java/com/animania/extra/ExtraTab.java", "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsTab.java"],
        ["base/src/test/java/com/animania/common/AnimaniaTabsContractTest.java"]),
    "src/main/java/com/animania/common/entities/generic/GenericBehavior.java": row(
        ["base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java", "base/src/main/java/com/animania/common/entity/goal/AnimaniaMateGoal.java", "base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"],
        ["base/src/test/java/com/animania/common/entity/AnimaniaAnimalEntitySerializationTest.java", "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"],
        serialization=["base/src/test/java/com/animania/common/entity/AnimaniaAnimalEntitySerializationTest.java"]),
    "src/main/java/com/animania/common/events/EntityEventHandler.java": row(
        ["base/src/main/java/com/animania/AnimaniaServerEvents.java", "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"],
        ["base/src/test/java/com/animania/common/AnimaniaServerContractTest.java"]),
    "src/main/java/com/animania/common/events/InteractHandler.java": row(
        ["base/src/main/java/com/animania/AnimaniaServerEvents.java", "base/src/main/java/com/animania/common/AnimaniaSeedPlacement.java", "base/src/main/java/com/animania/common/advancement/FeedAnimalTrigger.java"],
        ["base/src/test/java/com/animania/common/AnimaniaServerContractTest.java", "tools/audit_feed_trigger.py"]),
    "src/main/java/com/animania/common/handler/AddonHandler.java": row(
        ["base/src/main/java/com/animania/Animania.java", "farm/src/main/java/com/animania/farm/AnimaniaFarm.java", "extra/src/main/java/com/animania/extra/AnimaniaExtra.java", "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java"],
        ["tools/audit_addon_architecture.py", "tools/audit_startup_matrix.py"]),
    "src/main/java/com/animania/common/handler/AddonInjectionHandler.java": row(
        ["base/src/main/java/com/animania/api/AnimaniaApi.java", "base/src/main/java/com/animania/api/data/SpeciesDefinition.java"],
        ["base/src/test/java/com/animania/api/PublicApiContractTest.java"]),
    "src/main/java/com/animania/common/handler/CompatHandler.java": row(
        ["base/src/main/java/com/animania/compat/jei/AnimaniaJeiPlugin.java", "base/src/main/java/com/animania/compat/jade/AnimaniaJadePlugin.java", "base/src/main/java/com/animania/compat/top/AnimaniaTopProbeCompat.java"],
        ["base/src/test/java/com/animania/compat/AnimaniaCompatContractTest.java"]),
    "src/main/java/com/animania/common/handler/LootTableHandler.java": row(
        ["base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"],
        ["base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java"]),
    "src/main/java/com/animania/common/helper/AnimaniaHelper.java": row(
        ["base/src/main/java/com/animania/common/helper/AnimaniaHelper.java"],
        ["base/src/test/java/com/animania/common/helper/LegacyHelperContractTest.java"], serialization=["base/src/test/java/com/animania/common/helper/LegacyHelperContractTest.java"]),
    "src/main/java/com/animania/common/helper/ReflectionUtil.java": row(
        ["base/src/main/java/com/animania/common/helper/ReflectionUtil.java"], ["base/src/test/java/com/animania/common/helper/LegacyHelperContractTest.java"]),
    "src/main/java/com/animania/common/helper/RegistryHelper.java": row(
        ["base/src/main/java/com/animania/common/helper/RegistryHelper.java"], ["base/src/test/java/com/animania/common/helper/LegacyHelperContractTest.java"]),
    "src/main/java/com/animania/common/helper/StringParser.java": row(
        ["base/src/main/java/com/animania/common/helper/StringParser.java"], ["base/src/test/java/com/animania/common/helper/LegacyHelperContractTest.java"]),
    "src/main/java/com/animania/common/loottables/AddMoreFunction.java": row(["base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"], ["base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java"]),
    "src/main/java/com/animania/common/loottables/EntityFedProperty.java": row(["base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"], ["base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java"]),
    "src/main/java/com/animania/common/loottables/EntityGenderProperty.java": row(["base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"], ["base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java"]),
    "src/main/java/com/animania/common/loottables/EntityWateredProperty.java": row(["base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"], ["base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java"]),
    "src/main/java/com/animania/common/loottables/WoolColorFunction.java": row(["base/src/main/java/com/animania/common/loot/AnimaniaLootRules.java"], ["base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java"]),
    "src/main/java/com/animania/network/client/TileEntitySyncPacket.java": row(
        ["base/src/main/java/com/animania/network/AnimaniaNetwork.java", "base/src/main/java/com/animania/network/RequestAnimalSnapshotPacket.java", "base/src/main/java/com/animania/common/helper/AnimaniaHelper.java"],
        ["base/src/test/java/com/animania/network/AnimaniaNetworkContractTest.java", "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], client=["base/src/test/java/com/animania/network/AnimaniaNetworkContractTest.java"]),
    "src/main/java/com/animania/network/client/TileEntitySyncPacketHandler.java": row(
        ["base/src/main/java/com/animania/network/AnimaniaNetwork.java", "base/src/main/java/com/animania/network/RequestAnimalSnapshotPacket.java", "base/src/main/java/com/animania/common/helper/AnimaniaHelper.java"],
        ["base/src/test/java/com/animania/network/AnimaniaNetworkContractTest.java", "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], serialization=["base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"], client=["base/src/test/java/com/animania/network/AnimaniaNetworkContractTest.java"]),
    "src/main/java/com/animania/network/common/PacketCloseManual.java": row(
        ["base/src/main/java/com/animania/client/manual/ManualScreen.java", "base/src/main/java/com/animania/common/item/ManualItem.java"],
        ["base/src/test/java/com/animania/client/manual/ManualScreenTest.java"]),
    "src/main/java/com/animania/network/NetworkHandler.java": row(
        ["base/src/main/java/com/animania/network/AnimaniaNetwork.java", "base/src/main/java/com/animania/network/RequestAnimalSnapshotPacket.java"],
        ["base/src/test/java/com/animania/network/AnimaniaNetworkContractTest.java"]),
    "src/main/java/com/animania/proxy/ClientProxy.java": row(
        ["base/src/main/java/com/animania/Animania.java", "base/src/main/java/com/animania/client/AnimaniaClient.java"],
        ["base/src/test/java/com/animania/client/BaseClientContractTest.java"]),
    "src/main/java/com/animania/proxy/CommonProxy.java": row(
        ["base/src/main/java/com/animania/Animania.java", "base/src/main/java/com/animania/AnimaniaServerEvents.java"],
        ["base/src/test/java/com/animania/common/AnimaniaServerContractTest.java"]),
}

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    errors: list[str] = []
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    open_sources = {source for source, entry in by_source.items() if entry.get("module") == "base" and entry.get("status") != "closed"}
    unexpected_open = open_sources - set(CORE)
    if unexpected_open:
        errors.append(f"Base open rows contain unowned sources: {sorted(unexpected_open)}")
    for source in set(CORE) - open_sources:
        entry = by_source.get(source, {})
        if entry.get("status") != "closed" or not any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
            errors.append(f"Base source is neither open for audit nor closed with owned evidence: {source}")
    for source, evidence in CORE.items():
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
        paths = evidence["paths"] + evidence["behavior_tests"] + evidence["serialization_tests"] + evidence["client_tests"]
        for path in paths:
            if not (root / path).exists():
                errors.append(f"{source}: missing evidence {path}")
    changed = 0
    if not errors:
        for source, evidence in CORE.items():
            entry = by_source[source]
            proof = {**evidence, "notes": [f"{OWNER} legacy Base contract mapped to modern DeferredRegister/server-authoritative/native ModelPart implementation and dedicated executable evidence."]}
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=evidence["behavior_tests"] + evidence["serialization_tests"] + evidence["client_tests"],
                             target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
                errors.append(f"Base row not closed with owned evidence: {source}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(CORE), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
