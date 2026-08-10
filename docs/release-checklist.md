# 3.0.0 release gate

The release job must not set `release_allowed` until all of the following are
evidenced:

1. The migration matrix contains zero open or unverified entries.
2. All four independent JARs, sources JARs and SHA-256 files are generated.
3. Base-only, each single addon, every addon combination and the full install
   pass client/server startup, including the required missing-Base failure.
4. Unit tests and Forge GameTests cover every registered animal, facility,
   vehicle, recipe, loot table, advancement, language and API contract.
5. JEI, Jade and The One Probe are each tested alone and together as optional
   dependencies; no compatibility mod is required for startup.
6. Multiplayer, save/reload, dimension changes, chunk unloads and the 250
   animal 60-minute dedicated-server endurance run pass with no unhandled
   exceptions, duplication or loss.

