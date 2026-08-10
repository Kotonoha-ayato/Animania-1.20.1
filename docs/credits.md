# Credits and license audit

Animania 3.0.0 is distributed under **LGPL-3.0-or-later**; the complete text
is in the repository `LICENSE` file and is included in every published sources
JAR.  The port is based on the pinned `1.12` branch of
`capnkirok/animaniamod` (revision recorded in `docs/migration-matrix.json`),
with the `1.18` branch used only as an API/module naming reference.

The Animania Team, capnkirok and the upstream contributors retain authorship of
the original code and assets.  The 1.12 resource names and language coverage
are preserved where possible; converted native models/animations are marked in
the migration matrix.  The local CraftStudio archive used during audit is not
included in this published repository.  Forge, Minecraft and the optional
JEI, Jade and The One Probe integrations remain their respective authors'
copyrights and licenses; they are compile-only/optional and are not bundled.

The release JAR audit rejects CraftStudio, GeckoLib, Patchouli, CoFH and
Redstone Flux classes/resources so a published artifact cannot accidentally
claim or redistribute those runtimes.
