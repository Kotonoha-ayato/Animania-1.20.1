# Animania 1.20.1 migration guide and ledger

The machine-readable ledger is generated from the pinned 1.12 source tree by
`tools/build_migration_matrix.py`.  The 1.18 checkout is reference-only and is
never used as a build input.  Every source and resource entry carries an
implementation flag, a verification flag, and named tests; the release audit
must report zero open entries before version 3.0.0 can be published.

The target is Forge 47.4.22 on Java 17 and Minecraft 1.20.1.  The four mod IDs
are `animania`, `animania_farm`, `animania_extra`, and `animania_catsdogs`.

## World and configuration policy

Direct 1.12 world upgrades are intentionally unsupported: registry, entity and
block-entity serialization changed between Forge generations.  Make a backup,
start a fresh 1.20.1 world, and use the configuration converter for gameplay
settings:

```text
java -jar config-migrator/build/libs/animania-config-migrator-3.0.0.jar \
  --input <old-1.12-config-directory> --output <new-directory>
```

The input tree is read-only and the output directory must not already contain a
file that the converter would write.  The report separates values migrated to
Base, Farm, Extra and Cats&Dogs TOML files from values that were defaulted or
could not be represented.  It never edits a world, registry ID or player NBT.

## ID and resource mapping

`docs/id-mapping.json` preserves the `animania:*` legacy IDs where Forge allows
it and records module-qualified IDs for content that moved into an addon.  The
full source/resource/behavior ledger is `docs/migration-matrix.json`; entries
are closed only when target code/resources, a disposition (preserved,
rewritten, or native-model conversion) and a test/audit reference are present.

## Optional integrations

JEI, Jade and The One Probe are optional.  Base detects them at runtime and
registers recipe/info providers only when present; no compatibility mod is
required for a dedicated server.  See `docs/api.md` for the addon-facing API.
