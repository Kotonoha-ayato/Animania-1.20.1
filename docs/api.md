# Animania Java 17 API

The stable API is in `com.animania.api` in the Base artifact.  Addons should
compile against Base and register their content during their own mod
constructor; they must not reach into another addon's implementation package.

The Forge common config retains the legacy care/feeding controls plus typed
registry-ID lists for trough food, slop ingredients and food-value overrides;
the standalone converter emits these lists into the Base TOML section.

## Core contracts

- `IAnimaniaAnimal` exposes gender, age, variant, sleeping/playing state,
  hunger/thirst, pregnancy/gestation, sterilization, feeding, drinking,
  breeding and an immutable `AnimalSnapshot`.
- `AnimalGender` and `AnimalAge` are the canonical state enums.  Child entity
  types use the same species key as their adult type and grow server-side into
  the appropriate gendered adult registration.
- `SpeciesDefinition` describes an addon's species, family, dimensions and
  gestation duration.
- `AnimaniaApi.registerSpecies` publishes species metadata and
  `AnimaniaApi.registerFoodMatcher` lets an addon provide server-authoritative
  food matching without OreDictionary or a hard dependency on another mod.
- `AnimaniaApi.registerTamingRequirement` controls addon-specific breeding
  rules.  Query methods are safe when an addon is absent.

## Server authority and compatibility

Animal state is synchronized with `SynchedEntityData` and persisted in NBT.
Interaction, AI, breeding, pregnancy, birth, inventory and block-entity
mutations occur on the server.  `AnimaniaNetwork` carries snapshot requests
over a versioned Forge `SimpleChannel`; clients only render the received state.

Fluid and item automation use Forge capabilities (`IFluidHandler`,
`IItemHandler`) exposed by storage, troughs, hives, cheese molds, pet bowls and
vehicles.  Addons can query registered species and use the normal Forge tags
`animania:animal_feed`, `animania:animal_drink` and
`animania:breeding_food`.

## Binary compatibility policy

The 3.0.0 API is Java 17 and Forge 47.4.22.  Public types under
`com.animania.api` are kept free of client-only classes and Patchouli/JEI/Jade/
TOP types.  Future 3.x releases may add methods with defaults, but will not
remove or change the meaning of existing methods without a major version.
