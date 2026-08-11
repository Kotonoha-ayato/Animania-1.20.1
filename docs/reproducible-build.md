# Reproducible build

Requirements:

- Windows or Linux with a JDK 17 runtime (the repository's CI uses Temurin
  17), Python 3.10+ and the Gradle wrapper.
- Network access on the first run so ForgeGradle can cache Minecraft 1.20.1,
  Forge 47.4.22 and official mappings.  Subsequent builds use the Gradle
  cache.

On Windows, run Gradle with the repository as the current directory.  If the
checkout path contains non-ASCII characters and the test worker emits a
mojibake `ClassNotFoundException`, use an ASCII temporary checkout for the
build; do not interpret that worker failure as a test assertion result.

From the repository root:

```text
gradlew clean
python tools/build_migration_matrix.py --source upstream/Animania-1.12 --output docs/migration-matrix.json --reset-closure
python tools/test_closure_protocol.py
gradlew check runDataAll runGameTestsAll releaseBuild --max-workers=1
python tools/audit_api_contract.py --root . --output build/api-contract-audit.json
python tools/audit_config_converter_runtime.py --root . --jar config-migrator/build/libs/animania-config-migrator-3.0.0.jar --output build/config-converter-audit.json
python tools/audit_texture_resolver.py --root .
python tools/audit_client_smoke.py --root . --log <full-client-latest.log> --debug-log <full-client-debug.log.gz>
python tools/audit_resource_semantics.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_api_legacy_interfaces.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_breed_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_base_block_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_base_registry_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_farm_facility_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_farm_goal_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_farm_special_item_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_farm_vehicle_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_farm_child_growth_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_farm_fluid_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_extra_hamster_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_extra_spawn_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_sound_handler_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_animation_conversion.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_config_defaults.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_java_model_implementation.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_public_api_facade_implementation.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_documented_replacements.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/audit_generic_ai_behavior.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
# Requires a graphics-capable capture harness and a manifest with a real
# screenshot plus geometry/pose digest for every converted model entry.
python tools/audit_model_visual_regression.py --root . --manifest <capture-manifest.json> --output build/model-visual-regression.json
python tools/run_closure_audits.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/verify_release_gates.py --root . --write
python tools/apply_verified_closure.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence
python tools/verify_closure.py --root . --matrix docs/migration-matrix.json --evidence-dir build/audit-evidence --check-only
gradlew verifyRelease --max-workers=1
```

The pinned upstream revisions are checked out read-only by CI.  The Gradle JAR
and sources-JAR tasks use deterministic file order and zeroed entry timestamps;
rebuilding without source or toolchain changes produces identical SHA-256
values.  The artifact audit writes one `.sha256` file next to each main and
sources JAR, the standalone configuration migrator and its sources archive,
and records sizes/digests in `build/release-artifact-audit.json`.

`verify_release_gates.py` is intentionally fail-closed.  It writes a global
gate report but never writes `release_allowed`; absent model screenshots,
optional-mod, startup-matrix, multiplayer, or endurance artifacts remain false
until their real runs produce hashed evidence.
