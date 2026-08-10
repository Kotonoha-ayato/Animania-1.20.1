# Changelog

## 3.0.0 - client resource and advancement corrections

- Restored the server-authoritative `animania:feed_animal` advancement trigger; entering a world no longer grants every husbandry advancement.
- Converted all 221 addon animal registrations from the shared placeholder mesh to breed-specific native `ModelPart`/`LayerDefinition` geometry generated from the pinned 1.12 Java models.
- Added every dynamically registered animal egg item model and migrated item/block atlas paths to the singular 1.20.1 layout.
- Added modern slop fluid client textures/rendering and corrected the seed block state/model.
- Strengthened the release resource audit to reject tick criteria, plural atlas paths, missing registered item models, and missing breed model layers.

## 3.0.0 (Minecraft 1.20.1 / Forge 47.4.22)

This is the first Forge-only Java 17 release of the complete Animania 1.12
feature baseline.  It is split into four independent artifacts:

- `animania-base`: public API, shared animal state/AI, facilities, manual,
  native models and optional JEI/Jade/TOP bridges.
- `animania-farm`: farm animals, breeds, fluids, cheese, hives, food, tools
  and pullable/saddled vehicles.
- `animania-extra`: rabbits, peafowl, amphibians, hamsters and hamster-wheel
  automation.
- `animania-catsdogs`: cats, dogs, taming, vanilla companion replacement and
  pet facilities.

All addons require Base at runtime.  CraftStudio, GeckoLib, Patchouli,
CoFHCore and Redstone Flux are not runtime dependencies.  Legacy resources are
converted to native Forge/MC 1.20.1 data, models and animations; old worlds are
not upgraded in place.  Use the configuration migrator and migration report
when moving a 1.12 installation.

The release gate records the pinned 1.12 inventory in
`docs/migration-matrix.json`, runs unit tests and Forge GameTests, audits all
resources and JAR contents, and emits SHA-256 files for the four main and four
sources JARs.

Migration-specific notes:

- The native handbook reads all 140 Base/Farm/Extra/Cats&Dogs pages without
  Patchouli; addon pages are discovered from their legacy `animania/manual`
  resource path.
- The read-only converter preserves the legacy trough, slop, food override,
  hive, vehicle, lactation and egg settings, reporting defaults and
  unmigratable keys rather than silently dropping them.
- Farm wagon hitch UUIDs, egg projectiles, milk readiness and queued wild-hive
  decoration are server-authoritative and survive save/reload paths.
