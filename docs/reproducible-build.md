# Reproducible build

Requirements:

- Windows or Linux with a JDK 17 runtime (the repository's CI uses Temurin
  17), Python 3.10+ and the Gradle wrapper.
- Network access on the first run so ForgeGradle can cache Minecraft 1.20.1,
  Forge 47.4.22 and official mappings.  Subsequent builds use the Gradle
  cache.

From the repository root:

```text
gradlew clean
python tools/build_migration_matrix.py --source upstream/Animania-1.12 --output docs/migration-matrix.json
python tools/close_migration_matrix.py --root . --matrix docs/migration-matrix.json
gradlew verifyRelease releaseBuild
python tools/audit_resources.py --root .
python tools/audit_release.py --root . --version 3.0.0
```

The pinned upstream revisions are checked out read-only by CI.  The Gradle JAR
and sources-JAR tasks use deterministic file order and zeroed entry timestamps;
rebuilding without source or toolchain changes produces identical SHA-256
values.  The artifact audit writes one `.sha256` file next to each main and
sources JAR, the standalone configuration migrator and its sources archive,
and records sizes/digests in `build/release-artifact-audit.json`.
