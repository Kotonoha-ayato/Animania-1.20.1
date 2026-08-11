# Animania 3.0.0 release evidence

This report records the seven-stage plan evidence for Minecraft 1.20.1,
Forge 47.4.22 and Java 17. It does not convert an unrun external client or
endurance test into a pass.

## Migration closure (current fail-closed snapshot)

- Base source: `upstream/Animania-1.12` at
  `32ae2b4c56cb84284e865dae0d3b78770992ba1d`.
- API/module reference only: `upstream/Animania-1.18` at
  `3b8909c5404d0bad8f3437562ca0c3924c0fa46d`.
- The previous 2,033 closure claims are historical only and are not inherited.
  The current schema-v2 matrix has 2,033 entries; 710 are fully closed. The
  remaining rows are centrally classified instead of being reported as one
  misleading backlog: 1,039 are `client_verification_pending` (no code repair
  inferred), 284 are `nonclient_verification_pending` (implementation is
  proven but runtime evidence is missing), and 0 are `repair_required`.
  The central report computes
  `release_allowed=false`.
- Nine confirmed behavior gaps now have selector-bound GameTest evidence; this
  is partial evidence and does not close entries that still require integration,
  client, serialization, or other domains.
- Three small API data entries (AnimalContainer, EntityGender and Pose) and
  four legacy interface entries (IBlinking, ISpawnable, IFoodProviderTE and
  IFoodProviderBlock) are closed only from dedicated JUnit selectors. Four
  advancement files and the Extra sound catalog are also closed by explicit
  namespace-semantic proofs; the broader data-manager API facades remain open.
- Seventy-four farm/extra/cats-and-dogs breed entries have individual
  selector-bound Forge GameTest evidence; nine source entries without a
  registered child marker remain intentionally open. Six Farm mold models have
  semantic resource evidence only; their client geometry/pose requirements
  remain open until the real capture harness runs.
- Eleven Base block and block-entity entries now have dedicated multi-selector
  Forge GameTest evidence for troughs, nests, salt licks, mud, straw, seed
  piles and the invisible companion proxy; no matrix entry was closed by the
  GameTest runner itself.
- Two Base registry compatibility entries now have dedicated live selectors:
  the complete two-event sound registry and all legacy dictionary categories,
  including every dye color.
- Ten Farm facility/handler entries now have selector-bound tests covering
  wool variants, cheese blocks/molds, hives, milk/honey capabilities and
  persistence; all ten rows passed with no skipped selectors.
- Five Farm goal entries now have selector-bound tests covering the horse
  day/rider/pulling gates and live species-food temptation rules; all five
  rows passed with no skipped selectors.
- Seven Farm special-item entries now have selector-bound tests for egg
  throwing, milk/honey/soup consumption, fluid draining, cheese-wheel mapping
  and riding-crop durability; all seven rows passed with no skipped selectors.
- Seven Farm vehicle/inventory entries now have selector-bound tests for
  native cargo menus, puller and NBT state, tiller cultivation, item placement
  and gamerule-safe drops; all seven rows passed with no skipped selectors.
- Six Farm child-base entries now have an individually named family selector
  result covering every registered child-to-adult transition; both Farm fluid
  block entries also passed the live source/flowing registration and mold
  process test.
- The Extra spawn-handler entry now has two independent live selectors: the
  natural family-cap/spawn-egg bypass and the real Forge join-event rabbit
  replacement with config and UUID/family assertions.
- Eleven special Farm/Cats&Dogs breed entries now have parameterized JUnit
  evidence for their exact 1.12 egg-colour constants; breed behavior remains
  owned by the dedicated per-breed GameTest auditors.
- Farm and Extra sound handlers now have separate live selectors that enumerate
  all 96 and 52 legacy IDs respectively and verify active Forge registry
  bindings plus mod-bus integration.
- All eight CraftStudio animation resources now have deterministic conversion
  evidence: each pinned JSON clip was converted in an isolated temporary archive
  and matched against the native AnimationDefinition class, with the module
  bake/bone JUnit selector passing. This closes resource conversion only; visual
  pose capture remains an external client gate.
- Base, Farm, Extra and Cats&Dogs configuration entries now have independent
  source-derived default proofs against ForgeConfigSpec and ConfigMigrator,
  with the module registration and migration JUnit selectors passing.
- Ninety-seven Java model entries have independent isolated-generator method
  comparisons against their checked-in `LayerDefinition` targets. This is
  implementation evidence only; every corresponding visual/client requirement
  remains in `client_verification_pending` until a real capture proves it.
- Thirteen historical public API facades now have individual target and JUnit
  implementation evidence. Twelve retain an explicit behavior gap rather than
  treating the modern facade's existence as old runtime behavior parity.
- Twenty generic 1.12 AI classes are now individually bound to named native
  1.20.1 goals and unique markers from fresh Farm, Extra or Cats&Dogs Forge
  GameTest servers. This adds 20 full closures for verified goal behavior;
  the two unmapped generic AI classes remain open.
- `docs/id-mapping.json`: 358 legacy-to-modern mappings.

## Build environment

- Windows 10 amd64, Microsoft OpenJDK 17.0.17, Gradle Wrapper 8.8.
- Minecraft 1.20.1, Forge 47.4.22, official mappings `20230612.114412`.
- ForgeGradle tasks were run with `--max-workers=1` for deterministic shared
  development-JAR access; the two latest clean/sequential release builds were
  byte-identical.

## Artifacts

The four existing mod JARs are independent test builds. Farm, Extra and
Cats&Dogs require Base; JEI, Jade and The One Probe are optional. The digests
below are the current reproducible test-build artifacts; they must not be
treated as a formal 3.0.0 release until the central gate is green.

| artifact | bytes | SHA-256 |
| --- | ---: | --- |
| `animania-base-1.20.1-3.0.0.jar` | 2,798,477 | `a75961345b7d7e0b152c362639ac235ba8777f8a22615ea3925a2ab575884c39` |
| `animania-base-1.20.1-3.0.0-sources.jar` | 2,591,830 | `9c2f1da0ad77175aae4a9fd2abd574e57241750fe10cfe7ec3816257669ed52d` |
| `animania-farm-1.20.1-3.0.0.jar` | 6,304,051 | `55a4f61641f5e555d8f8982c250767c5f612fd7383dc8cb4e203868d5e89d4d9` |
| `animania-farm-1.20.1-3.0.0-sources.jar` | 10,632,598 | `40087fb6adb524364064b82381b209d558fe3800f231321818dc75b579f3ccf6` |
| `animania-extra-1.20.1-3.0.0.jar` | 1,856,493 | `440d291cec0c8663a64066fcf32399c69876bb7169f23dd0e67d1b02fcc3bea7` |
| `animania-extra-1.20.1-3.0.0-sources.jar` | 3,275,912 | `121ed4474ad18760b24bb520c6469bf6aaf6d86162b3f027a312b5bc97094dbe` |
| `animania-catsdogs-1.20.1-3.0.0.jar` | 717,213 | `b0bd2b018ff7a08f43e5410c3add356e8d4f9135d4ba03ec0686e60027bce0f9` |
| `animania-catsdogs-1.20.1-3.0.0-sources.jar` | 862,030 | `830ef96b149a4b72b0f80b9d521c653ee63233271af1dd1dd33048b8892ecc00` |
| `animania-config-migrator-3.0.0.jar` | 19,367 | `87bf9e8118fbaac2004b7560fe4806d1bd34ff3679ef0c53c57f258c246e19c2` |
| `animania-config-migrator-3.0.0-sources.jar` | 10,812 | `0b218335b80216925131da9ba1fe036f88955b832f258651b8b62d9949b804b0` |

All archives contain `META-INF/LICENSE` and `META-INF/credits.md`. The
artifact audit found no CraftStudio, GeckoLib, Patchouli, CoFH or Redstone
Flux content.

## Automated evidence (not a release approval)

The following all passed with the Gradle wrapper and `--max-workers=1` where
ForgeGradle shares a Base development JAR:

- Unit tests: Base, Farm, Extra, Cats&Dogs and config-migrator.
- Data generation: `runData` for all modules.
- Forge GameTests: full-install Base run 110/110, Farm 56/56, Extra 26/26,
  Cats&Dogs 12/12; the logs are used only where selector markers bind them to
  a specific matrix requirement.
- Schema-v2 protocol tests, read-only resource/Java audits and central closure
  validation.  `verifyRelease` remains intentionally fail-closed while 1,376
  requirements are open.
- Resource closure: Farm 102, Extra 53, Cats&Dogs 69 entity textures;
  140 native manual pages; 25 locale files per module; native ModelPart and
  eight AnimationDefinition clips; 150 sound events matched to the 1.12
  baseline.
- Migration closure check: 710/2,033 closed; no release approval. Open rows
  are centrally split into 1,039 client-only/client-plus-compat verification
  rows, 284 non-client execution rows, and 0 rows requiring concrete repair.
- Texture resolver audit: 352 registered ID/variant/sheared combinations checked,
  352 resolved to an existing nested resource or an explicit flat fallback.
- Client bootstrap smoke: the latest all-module Forge client reached resource
  reload and block-atlas initialization with all four mod IDs and no model
  construction errors. This is not a substitute for the required per-breed
  screenshot/geometry regression.
- API contract audit: all required animal state and addon-query methods are
  source-fingerprint checked and bound to six passing Base JUnit selectors.
- Configuration converter runtime audit: a packaged-jar migration preserved
  input bytes, emitted four module TOMLs plus an itemized report, and rejected
  a second invocation that would overwrite output.

## External-environment evidence still required

The repository gate is not green. A graphics-capable client with
JEI/Jade/TOP, two real clients, and a dedicated server with a 250-animal
60-minute endurance workload must still be run in the target deployment
environment; those runs cannot be truthfully simulated by a headless build.
They are environmental evidence, not code or migration-matrix gaps.

The model-visual gate is also intentionally false until a capture manifest
contains real screenshots and geometry/pose digests for all 130 converted model
entries. A passing structural inventory or a non-empty ModelPart is not enough.

## Main change areas

- `base/`: stable `com.animania.api`, server-authoritative state/AI, NBT and
  network, native model/animation, manual and optional integrations.
- `farm/`, `extra/`, `catsdogs/`: independent registries, entities, variants,
  facilities, fluids/vehicles, configs and GameTests.
- `tools/`: locale/resource/ID/migration closure, release and startup audits.
- `config-migrator/`: read-only 1.12 configuration conversion and JSON report.
- `docs/`, `CHANGELOG.md`, `LICENSE`: migration, API, reproducibility, credits
  and release evidence.
