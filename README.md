# Animania Forge 1.20.1

Animania 3.0.0 is a Forge-only, Java 17 multi-project port of the complete
1.12 feature set.  The build targets Minecraft 1.20.1 with Forge 47.4.22 and
produces four independent mod JARs:

* `animania-base-1.20.1-3.0.0.jar`
* `animania-farm-1.20.1-3.0.0.jar`
* `animania-extra-1.20.1-3.0.0.jar`
* `animania-catsdogs-1.20.1-3.0.0.jar`

Farm, Extra and Cats&Dogs declare a mandatory runtime dependency on Base.
The published repository contains only the 1.20.1 port.  The 1.12 baseline
and 1.18 API reference checkouts used during migration are local/CI-only
inputs and are not distributed here.  CraftStudio and GeckoLib are not
dependencies.

## Reproducible build

Use JDK 17 and run `gradlew releaseBuild`.  The build also creates sources
JARs.  `gradlew verifyRelease` runs the automated checks available in the
checkout.  The complete release gate additionally requires a Forge client,
integrated server, dedicated server, GameTest, compatibility, multiplayer and
60-minute endurance run as described in `docs/migration.md` and the release
checklist.

## Configuration migration

The converter is read-only with respect to old files and refuses to overwrite
anything in the output directory:

```text
java -jar config-migrator/build/libs/animania-config-migrator-3.0.0.jar \
  --input <1.12-config-dir> --output <new-dir>
```

## License

Code is distributed under LGPL-3.0-or-later.  Upstream authors and third-party
resource credits are retained in the repository and release notes.
