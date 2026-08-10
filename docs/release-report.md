# Animania 3.0.0 release evidence

This report records the seven-stage plan evidence for Minecraft 1.20.1,
Forge 47.4.22 and Java 17. It does not convert an unrun external client or
endurance test into a pass.

## Migration closure

- Base source: `upstream/Animania-1.12` at
  `32ae2b4c56cb84284e865dae0d3b78770992ba1d`.
- API/module reference only: `upstream/Animania-1.18` at
  `3b8909c5404d0bad8f3437562ca0c3924c0fa46d`.
- `docs/migration-matrix.json`: 2,033 entries; unstarted 0, open 0,
  unverified 0, closed 2,033; `release_allowed=true`.
- `docs/id-mapping.json`: 358 legacy-to-modern mappings.

## Build environment

- Windows 10 amd64, Microsoft OpenJDK 17.0.17, Gradle Wrapper 8.8.
- Minecraft 1.20.1, Forge 47.4.22, official mappings `20230612.114412`.
- ForgeGradle tasks were run with `--max-workers=1` for deterministic shared
  development-JAR access; the two latest clean/sequential release builds were
  byte-identical.

## Artifacts

The four mod JARs are independent. Farm, Extra and Cats&Dogs require Base;
JEI, Jade and The One Probe are optional. The following sizes and SHA-256
values were identical across two clean sequential release builds.

| artifact | bytes | SHA-256 |
| --- | ---: | --- |
| `animania-base-1.20.1-3.0.0.jar` | 2,472,284 | `70ca4709f7664ccc44105523ffeedf96c563e03eb33f321d645a173d9adf6bf3` |
| `animania-base-1.20.1-3.0.0-sources.jar` | 2,400,118 | `94ab6ac861cbd34b2637b53bad306fa013ac34cd6cd6d3030891686cee5cda54` |
| `animania-farm-1.20.1-3.0.0.jar` | 9,992,091 | `26f7507ea68d9fa6da72dc0c011d1d3581f9f8f300c977017815aaf3e4ccb671` |
| `animania-farm-1.20.1-3.0.0-sources.jar` | 9,921,820 | `f258bce679abccb27b36836a5dec468c6558825647b532124892ac070b6d6b27` |
| `animania-extra-1.20.1-3.0.0.jar` | 3,195,815 | `051a7f77f13f887e08a7ed68ab8c4fade524f65d74bf3cc1012decfec43fafc7` |
| `animania-extra-1.20.1-3.0.0-sources.jar` | 3,164,948 | `3910bf95b985845bd3a5cc4a8f76cb9741d52aa16501dfb4cce8ec28d264cc3c` |
| `animania-catsdogs-1.20.1-3.0.0.jar` | 778,443 | `d4ae526b9d4f3b03302c998d59772eb146465c00361c036f2e40267b85330c9c` |
| `animania-catsdogs-1.20.1-3.0.0-sources.jar` | 751,329 | `ee7b195d67a7dea82d31eefd228a23e4932e59b8841c1851a846b315e493752e` |
| `animania-config-migrator-3.0.0.jar` | 18,811 | `dd30d4f7bc3168994f7b5e55fc331be981302637a2a0dc81a8d08b9365ad743e` |
| `animania-config-migrator-3.0.0-sources.jar` | 10,553 | `3201d9fd18ac97a319debaeed8b3e9562e6030e805f3804e17db7c71543985df` |

All archives contain `META-INF/LICENSE` and `META-INF/credits.md`. The
artifact audit found no CraftStudio, GeckoLib, Patchouli, CoFH or Redstone
Flux content.

## Automated evidence

The following all passed with the Gradle wrapper and `--max-workers=1` where
ForgeGradle shares a Base development JAR:

- Unit tests: Base, Farm, Extra, Cats&Dogs and config-migrator.
- Data generation: `runData` for all modules.
- Forge GameTests: Base 6/6, Farm 12/12, Extra 5/5, Cats&Dogs 4/4.
- `verifyRelease`, resource audit, release artifact audit and startup matrix.
- Resource closure: Farm 102, Extra 53, Cats&Dogs 69 entity textures;
  140 native manual pages; 25 locale files per module; native ModelPart and
  eight AnimationDefinition clips; 150 sound events matched to the 1.12
  baseline.
- Migration closure check: 2,033/2,033 closed.
- Base-only dedicated-server smoke reached Forge `Done` before the process was
  stopped; two sequential `releaseBuild --rerun-tasks` runs had identical
  artifact sizes and digests.

## External-environment evidence still required

The repository gate is green for source, data, unit, GameTest, dependency
metadata, artifact and reproducibility checks. A graphics-capable client with
JEI/Jade/TOP, two real clients, and a dedicated server with a 250-animal
60-minute endurance workload must still be run in the target deployment
environment; those runs cannot be truthfully simulated by a headless build.
They are environmental evidence, not code or migration-matrix gaps.

## Main change areas

- `base/`: stable `com.animania.api`, server-authoritative state/AI, NBT and
  network, native model/animation, manual and optional integrations.
- `farm/`, `extra/`, `catsdogs/`: independent registries, entities, variants,
  facilities, fluids/vehicles, configs and GameTests.
- `tools/`: locale/resource/ID/migration closure, release and startup audits.
- `config-migrator/`: read-only 1.12 configuration conversion and JSON report.
- `docs/`, `CHANGELOG.md`, `LICENSE`: migration, API, reproducibility, credits
  and release evidence.
