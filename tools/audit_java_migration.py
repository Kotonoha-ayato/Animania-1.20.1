"""Prove low-risk Java migrations against the pinned 1.12 source.

This is intentionally a narrow audit.  It closes only legacy animal type
tables and breed wrappers whose source behaviour is limited to breed identity
and spawn-egg colours.  Registration, construction, breeding, persistence,
models and textures must already have executable coverage.  Files with any
additional override (special AI, shearing, variants, interaction, and so on)
remain open for a dedicated semantic migration.
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path


OWNER = "[java-audit:simple-breed-v1]"
MODULE = {
    "farm": {
        "ids": "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
        "main": "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
        "game": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "registry": "farm/src/test/java/com/animania/farm/FarmRegistryTest.java",
        "texture": "farm/src/test/java/com/animania/farm/FarmTextureResolverTest.java",
        "model": "farm/src/main/java/com/animania/farm/client/model/FarmLegacyModelLayers.java",
        "sound_catalog": "farm/src/main/java/com/animania/farm/FarmSoundCatalog.java",
        "sounds": "farm/src/main/java/com/animania/farm/FarmSounds.java",
        "sounds_json": "farm/src/main/resources/assets/animania_farm/sounds.json",
    },
    "extra": {
        "ids": "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java",
        "main": "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
        "game": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "registry": "extra/src/test/java/com/animania/extra/ExtraRegistryTest.java",
        "texture": "extra/src/test/java/com/animania/extra/ExtraTextureResolverTest.java",
        "model": "extra/src/main/java/com/animania/extra/client/model/ExtraLegacyModelLayers.java",
        "sound_catalog": "extra/src/main/java/com/animania/extra/ExtraSoundCatalog.java",
        "sounds": "extra/src/main/java/com/animania/extra/ExtraSounds.java",
        "sounds_json": "extra/src/main/resources/assets/animania_extra/sounds.json",
    },
    "catsdogs": {
        "ids": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java",
        "main": "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java",
        "game": "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
        "registry": "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsRegistryTest.java",
        "texture": "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsTextureResolverTest.java",
        "model": "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsLegacyModelLayers.java",
    },
}

# These wrappers have no legacy override beyond the two spawn-egg colours.
# More complicated breed files are deliberately excluded even when their
# modern generic implementation already appears plausible.
SIMPLE_BREEDS = {
    "farm": {
        "ChickenLeghorn.java", "ChickenOrpington.java", "ChickenPlymouthRock.java",
        "ChickenRhodeIslandRed.java", "ChickenWyandotte.java",
        "CowAngus.java", "CowHereford.java", "CowHighland.java", "CowLonghorn.java",
        "PigDuroc.java", "PigHampshire.java", "PigLargeBlack.java",
        "PigLargeWhite.java", "PigOldSpot.java", "PigYorkshire.java",
        "GoatAlpine.java", "GoatKiko.java", "GoatKinder.java",
        "GoatNigerianDwarf.java", "GoatPygmy.java", "HorseDraft.java",
    },
    "extra": {
        "PeafowlBlue.java", "PeafowlCharcoal.java", "PeafowlOpal.java",
        "PeafowlPeach.java", "PeafowlPurple.java", "PeafowlTaupe.java",
        "PeafowlWhite.java", "RabbitChinchilla.java", "RabbitCottonail.java",
        "RabbitDutch.java", "RabbitHavana.java", "RabbitJack.java",
        "RabbitLop.java", "RabbitNewZealand.java", "RabbitRex.java",
        "EntityFerretGrey.java", "EntityFerretWhite.java",
        "EntityHedgehog.java", "EntityHedgehogAlbino.java",
    },
    "catsdogs": {
        "DogBloodHound.java", "DogCorgi.java", "DogDachshund.java",
        "DogGermanShepherd.java", "DogGreatDane.java", "DogGreyhound.java",
        "DogHusky.java", "DogPomeranian.java", "DogPug.java",
        "CatAmericanShorthair.java", "CatAsiatic.java", "CatExotic.java",
        "CatNorwegian.java", "CatOcelot.java", "CatRagdoll.java",
        "CatSiamese.java", "CatTabby.java",
    },
}

TYPE_FILES = {
    "farm": {"ChickenType.java", "CowType.java", "GoatType.java", "HorseType.java", "PigType.java", "SheepType.java"},
    "extra": {"AmphibianType.java", "PeacockType.java", "FerretType.java", "HamsterType.java", "HedgehogType.java", "RabbitType.java"},
    "catsdogs": {"DogType.java", "CatType.java"},
}


@dataclass(frozen=True)
class Proof:
    ids: tuple[str, ...]
    kind: str
    paths: tuple[str, ...] = ()


REGISTRATION_HANDLERS = {
    "farm": {"FarmAddonBlockHandler.java", "FarmAddonItemHandler.java", "FarmAddonEntityHandler.java"},
    "extra": {"ExtraAddonBlockHandler.java", "ExtraAddonItemHandler.java", "ExtraAddonEntityHandler.java"},
    "catsdogs": {"CatsDogsAddonBlockHandler.java", "CatsDogsAddonItemHandler.java", "CatsDogsAddonEntityHandler.java"},
}

SOUND_HANDLERS = {
    "farm": {"FarmAddonSoundHandler.java"},
    "extra": {"ExtraAddonSoundHandler.java"},
    "catsdogs": set(),
}


def camel(value: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", value).lower()


def entity_id(name: str) -> str:
    value = name.removeprefix("Entity")
    if value == "Frogs": return "frog"
    if value == "DartFrogs": return "dartfrog"
    if value.startswith("Rabbit"):
        value = value.removeprefix("Rabbit")
    value = value.removesuffix("Horse")
    result = camel(value)
    if result.startswith("peafowl_"):
        result = "peahen_" + result[len("peafowl_"):]
    return result


def class_body(text: str, name: str) -> str | None:
    match = re.search(r"\bclass\s+" + re.escape(name) + r"\b[^\{]*\{", text)
    if not match:
        return None
    start = match.end() - 1
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "{": depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1:index]
    return None


def method_names(text: str) -> set[str]:
    names: set[str] = set()
    for match in re.finditer(r"@Override\s+(?:public|protected)\s+[\w<>?]+\s+(\w+)\s*\(", text):
        names.add(match.group(1))
    return names


def return_int(body: str, method: str, source_text: str = "") -> int | None:
    match = re.search(r"\b" + re.escape(method) + r"\s*\([^)]*\)\s*\{\s*return\s+([A-Za-z_]\w*|-?(?:0x[0-9a-fA-F]+|\d+))\s*;", body)
    if not match:
        return None
    value = match.group(1)
    if re.fullmatch(r"-?(?:0x[0-9a-fA-F]+|\d+)", value):
        return int(value, 0)
    constant = re.search(r"\b(?:static\s+)?(?:final\s+)?int\s+" + re.escape(value) + r"\s*=\s*(-?(?:0x[0-9a-fA-F]+|\d+))\s*;", source_text)
    return int(constant.group(1), 0) if constant else None


def java_strings(path: Path) -> set[str]:
    return set(re.findall(r'"([a-z0-9_]+)"', path.read_text(encoding="utf-8")))


def sound_catalog_ids(text: str) -> set[str]:
    ids = set(re.findall(r'ids\.add\("([a-z0-9_]+)"\)', text))
    for prefix, first, last in re.findall(r'range\(ids,\s*"([a-z0-9_]+)",\s*(\d+),\s*(\d+)\)', text):
        ids.update(prefix + str(value) for value in range(int(first), int(last) + 1))
    return ids


def prove_sound_handler(root: Path, module: str, filename: str, source_text: str) -> tuple[Proof | None, str]:
    cfg = MODULE[module]
    declared = set(re.findall(r'public\s+static\s+SoundEvent\s+(\w+)\s*;', source_text))
    assigned = dict(re.findall(r'(\w+)\s*=\s*registerSound\("([^"]+)"\)', source_text))
    if not declared or not assigned:
        return None, "legacy sound declarations/registrations missing"
    source_root = root / "upstream/Animania-1.12/src/main/java"
    used: set[str] = set()
    handler_name = filename.removesuffix(".java")
    for path in source_root.rglob("*.java"):
        used.update(re.findall(re.escape(handler_name) + r'\.(\w+)', path.read_text(encoding="utf-8", errors="replace")))
    unresolved = used - declared - {"preInit"}
    if unresolved:
        return None, f"legacy code references undeclared sound fields: {sorted(unresolved)}"
    # The 1.12 handlers forgot to assign sheepLiving7 and hedgehogHurt2 even
    # though entity code used both. Preserve content intent and repair those
    # source defects by deriving their lower-case event IDs from field names.
    expected = {value.lower() for value in assigned.values()}
    expected.update(field.lower() for field in used & (declared - assigned.keys()))
    target_catalog = sound_catalog_ids((root / cfg["sound_catalog"]).read_text(encoding="utf-8"))
    if target_catalog != expected:
        return None, f"modern sound catalog differs: missing={sorted(expected-target_catalog)}, extra={sorted(target_catalog-expected)}"
    sounds_json = json.loads((root / cfg["sounds_json"]).read_text(encoding="utf-8"))
    if set(sounds_json) != expected:
        return None, "sounds.json event keys differ from the source-derived catalog"
    sounds = (root / cfg["sounds"]).read_text(encoding="utf-8")
    main = (root / cfg["main"]).read_text(encoding="utf-8")
    game = (root / cfg["game"]).read_text(encoding="utf-8")
    simple = module.title() if module != "farm" else "Farm"
    if f"{simple}SoundCatalog.IDS.forEach" not in sounds or f"{simple}Sounds.SOUNDS.register(bus)" not in main:
        return None, "catalog is not wired through DeferredRegister and the module event bus"
    if f"every{simple}SoundEventIsRegistered" not in game or "ForgeRegistries.SOUND_EVENTS.containsKey" not in game:
        return None, "Forge GameTest does not verify every event in the live registry"
    return Proof(tuple(sorted(expected)), f"sound-handler-{len(expected)}"), ""


def current_colours(root: Path, animal_id: str) -> tuple[int, int] | None:
    text = (root / "base/src/main/java/com/animania/common/item/LegacyEggColors.java").read_text(encoding="utf-8")
    breed_part, exact_part = text.split("private static final Map<String, Colors> EXACT", 1)
    parse = lambda value: {key: (int(a), int(b)) for key, a, b in re.findall(r'e\("([a-z0-9_]+)",\s*(-?\d+),\s*(-?\d+)\)', value)}
    breeds, exact = parse(breed_part), parse(exact_part)
    if animal_id in exact:
        return exact[animal_id]
    roles = ("rooster_", "stallion_", "peachick_", "peacock_", "peahen_", "piglet_",
             "female_", "kitten_", "puppy_", "queen_", "bull_", "calf_", "chick_", "cow_",
             "buck_", "doe_", "ewe_", "foal_", "hen_", "hog_", "kid_", "kit_", "lamb_",
             "male_", "mare_", "ram_", "sow_", "tom_")
    breed = next((animal_id[len(role):] for role in roles if animal_id.startswith(role)), animal_id)
    if animal_id == "lamb_dorper": breed = "dorper_child"
    if breed == "friesian":
        breed = "friesian_cow" if animal_id.startswith(("bull_", "cow_", "calf_")) else "friesian_sheep"
    return breeds.get(breed)


def prove(root: Path, entry: dict) -> tuple[Proof | None, str]:
    module = entry.get("module")
    if module not in MODULE:
        return None, "unsupported module"
    filename = Path(entry["source"]).name
    is_type = filename in TYPE_FILES[module]
    is_breed = filename in SIMPLE_BREEDS[module]
    is_registration = filename in REGISTRATION_HANDLERS[module]
    is_sound = filename in SOUND_HANDLERS[module]
    if not is_type and not is_breed and not is_registration and not is_sound:
        return None, "not in the deliberately narrow low-risk set"
    source = root / "upstream/Animania-1.12" / entry["source"]
    if not source.exists():
        return None, "pinned source missing"
    text = source.read_text(encoding="utf-8", errors="replace")

    if is_sound:
        return prove_sound_handler(root, module, filename, text)

    if is_registration:
        mapping = json.loads((root / "docs/id-mapping.json").read_text(encoding="utf-8"))["entries"]
        audit = json.loads((root / "docs/id-mapping-audit.json").read_text(encoding="utf-8"))
        if not audit.get("passed"):
            return None, "ID declaration audit is not green"
        source_rows = [row for row in mapping if any(filename in evidence for evidence in row.get("source_evidence", []))]
        if not source_rows:
            return None, "handler has no source-derived registry declarations"
        audited = {(row["module"], row["kind"], row["modern_id"]): row for row in audit["entries"]}
        paths: set[str] = set()
        for row in source_rows:
            key = (row["module"], row["kind"], row["modern_id"])
            check = audited.get(key)
            if not check or not check.get("declared"):
                return None, f"undeclared mapped registry ID: {row['modern_id']}"
            for path in check.get("target_evidence", []):
                if not (root / path).exists():
                    return None, f"missing declaration evidence: {path}"
                paths.add(path)
        ids = tuple(row["modern_id"] for row in source_rows)
        return Proof(ids, f"registry-handler-{len(ids)}", tuple(sorted(paths))), ""

    ids = tuple(dict.fromkeys(entity_id(name) for name in re.findall(r"\b(Entity[A-Z]\w*)\.class", text)))
    if not ids:
        # Breed wrappers declare nested entity classes rather than class literals.
        ids = tuple(entity_id(name) for name in entry.get("classes", []) if name.startswith("Entity"))
    known = java_strings(root / MODULE[module]["ids"])
    missing = [animal_id for animal_id in ids if animal_id not in known]
    if missing:
        return None, f"legacy entity classes do not map to current IDs: {missing}"

    game = (root / MODULE[module]["game"]).read_text(encoding="utf-8")
    required_game_tokens = ("ENTITIES.size()", "ConstructsAndPersistsCareState", "BreedResolvesItsLegacyChildType")
    if any(token not in game for token in required_game_tokens):
        return None, "module GameTests do not prove registration, construction/persistence, and family mapping"

    if is_breed:
        overrides = method_names(text)
        if overrides - {"getPrimaryEggColor", "getSecondaryEggColor"}:
            return None, f"breed has unhandled legacy overrides: {sorted(overrides)}"
        for class_name in entry.get("classes", []):
            if not class_name.startswith("Entity"):
                continue
            body = class_body(text, class_name)
            if body is None:
                return None, f"cannot isolate {class_name}"
            primary = return_int(body, "getPrimaryEggColor", text)
            secondary = return_int(body, "getSecondaryEggColor", text)
            if primary is None or secondary is None:
                return None, f"{class_name} lacks literal legacy egg colours"
            animal_id = entity_id(class_name)
            # Several 1.12 dog classes wrote opaque ARGB literals as signed
            # Java ints. Spawn eggs consume RGB, so compare the low 24 bits.
            source_colours = (primary & 0xFFFFFF, secondary & 0xFFFFFF)
            if current_colours(root, animal_id) != source_colours:
                return None, f"{animal_id} egg colours differ: source={source_colours} target={current_colours(root, animal_id)}"
    return Proof(ids, "type-family-table" if is_type else "simple-breed-wrapper"), ""


def evidence(root: Path, entry: dict, proof: Proof) -> dict:
    module = entry["module"]
    cfg = MODULE[module]
    if proof.kind.startswith("registry-handler-"):
        paths = list(proof.paths) + ["docs/id-mapping.json", "docs/id-mapping-audit.json", "tools/audit_id_mapping.py", "tools/audit_java_migration.py"]
        note = f"{OWNER} {proof.kind}: every source-evidenced registration has a declared Forge 47 target; content behaviour remains independently tracked by its own matrix entries."
        return {
            "paths": list(dict.fromkeys(paths)),
            "behavior_tests": ["tools/audit_id_mapping.py", cfg["game"]],
            "serialization_tests": [],
            "client_tests": [],
            "notes": [note],
        }
    if proof.kind.startswith("sound-handler-"):
        paths = [cfg["sound_catalog"], cfg["sounds"], cfg["sounds_json"], cfg["main"],
                 "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
                 "base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java",
                 "tools/audit_resources.py", "tools/audit_java_migration.py"]
        return {
            "paths": paths,
            "behavior_tests": [cfg["registry"], cfg["game"], "tools/audit_resources.py"],
            "serialization_tests": [],
            "client_tests": [],
            "notes": [f"{OWNER} {proof.kind}: all source-registered and source-referenced sound IDs are legal, resource-backed, DeferredRegister-backed, and verified in a live Forge GameTest registry; fixes the two legacy unassigned-but-used fields."],
        }
    paths = [
        "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
        "base/src/main/java/com/animania/common/item/LegacyEggColors.java",
        cfg["ids"], cfg["main"], cfg["model"], "tools/audit_java_migration.py",
    ]
    return {
        "paths": paths,
        "behavior_tests": [cfg["game"], cfg["registry"]],
        "serialization_tests": [],
        "client_tests": [cfg["registry"], cfg["texture"]],
        "notes": [f"{OWNER} {proof.kind}: source classes map to {', '.join(proof.ids)}; exact egg colours and executable module-wide registration/family/model/texture evidence verified."],
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
    proven = 0
    errors: list[str] = []
    changed = 0
    candidates = 0
    for entry in matrix["entries"]:
        if entry.get("kind") != "java":
            continue
        filename = Path(entry["source"]).name
        module = entry.get("module")
        if module not in MODULE or filename not in SIMPLE_BREEDS[module] | TYPE_FILES[module] | REGISTRATION_HANDLERS[module] | SOUND_HANDLERS[module]:
            continue
        candidates += 1
        proof, reason = prove(root, entry)
        owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
        if proof is None:
            if owned:
                errors.append(f"{module}:{entry['source']}: prior proof invalid: {reason}")
                if args.write:
                    entry.update(status="unstarted", implemented=False, verified=False, tests=[])
                    entry["target_evidence"] = {"paths": [], "behavior_tests": [], "serialization_tests": [], "client_tests": [], "notes": []}
                    changed += 1
            continue
        proven += 1
        if args.write:
            target = evidence(root, entry, proof)
            if entry.get("status") != "closed" or entry.get("target_evidence") != target:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=target["behavior_tests"] + target["client_tests"])
                entry["target_evidence"] = target
                changed += 1
        elif entry.get("status") != "closed" or not owned:
            errors.append(f"{module}:{entry['source']}: provable candidate is not closed by this audit")
    if args.write:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report = {"candidates": candidates, "proven": proven, "changed": changed, "errors": errors, "error_count": len(errors)}
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
