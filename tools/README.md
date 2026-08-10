# Migration tooling

`build_migration_matrix.py` reads the pinned 1.12 checkout and emits a JSON
inventory with source hashes, module ownership, candidate legacy IDs and
explicit implementation/verification fields.  It never changes the upstream
checkout.  A release is prohibited while any entry is `unstarted`, `open`, or
unverified.

Example:

```text
python tools/build_migration_matrix.py \
  --source upstream/Animania-1.12 \
  --output docs/migration-matrix.json
```

Additional release checks:

* `normalize_legacy_resources.py` rewrites 1.12 recipe serializers and model
  namespaces into 1.20.1-compatible JSON.
* `ensure_texture_aliases.py` records deterministic aliases for unresolved
  legacy texture names in `build/texture-aliases.json`.
* `ensure_locales.py` emits the 25-locale JSON surface for every module.
* `audit_resources.py` is the strict resource/data gate used by Gradle.
* `build_id_mapping.py` writes `docs/id-mapping.json` for registry migration.
