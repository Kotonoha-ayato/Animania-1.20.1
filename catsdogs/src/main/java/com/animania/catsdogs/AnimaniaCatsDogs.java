package com.animania.catsdogs;

import com.animania.api.AnimaniaApi;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(AnimaniaCatsDogs.MOD_ID)
public final class AnimaniaCatsDogs {
    public static final String MOD_ID = "animania_catsdogs";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final Map<String, RegistryObject<EntityType<?>>> ENTITIES = new LinkedHashMap<>();

    static { CatsDogsLegacyIds.ALL.forEach(AnimaniaCatsDogs::register); }

    private static void register(String id) {
        RegistryObject<EntityType<?>> registered = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.of(AnimaniaAnimalEntity::new, MobCategory.CREATURE)
                        .sized(sizeFor(id, true), sizeFor(id, false)).clientTrackingRange(8).updateInterval(3)
                        .build(MOD_ID + ":" + id));
        ENTITIES.put(id, registered);
        AnimaniaApi.registerSpecies(new SpeciesDefinition(new ResourceLocation(MOD_ID, id), family(id), gender(id), sizeFor(id, true), sizeFor(id, false), 20000));
    }

    public AnimaniaCatsDogs() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
        CatsDogsContent.ITEMS.register(bus);
        CatsDogsContent.BLOCKS.register(bus);
        CatsDogsContent.BLOCK_ENTITIES.register(bus);
        CatsDogsTab.TABS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CatsDogsConfig.SPEC);
        AnimaniaApi.registerTamingRequirement(MOD_ID, () -> CatsDogsConfig.REQUIRE_TAMING_FOR_BREEDING.get());
        AnimaniaApi.registerFoodMatcher(MOD_ID, (id, stack) -> {
            String path = id.getPath();
            boolean cat = path.startsWith("queen_") || path.startsWith("tom_") || path.startsWith("kitten_");
            return cat ? CatsDogsConfig.matchesCatFood(stack) : CatsDogsConfig.matchesDogFood(stack);
        });
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerGameTests);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaCatsDogs::replaceVanillaCompanion);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaCatsDogsClient::onClientSetup));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        ENTITIES.values().forEach(type -> event.put((EntityType<? extends LivingEntity>) type.get(), AnimaniaAnimalEntity.createAttributes().build()));
    }

    private void spawnPlacements(SpawnPlacementRegisterEvent event) {
        // Spawn placement registration can precede Forge common-config loading in
        // GameTest/dev startup, so use the default until the value is available.
        if (!spawnsEnabled()) return;
        ENTITIES.values().forEach(type -> event.register((EntityType<? extends AnimaniaAnimalEntity>) type.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AnimaniaAnimalEntity::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR));
    }

    private static boolean spawnsEnabled() {
        try {
            return CatsDogsConfig.ENABLE_SPAWNS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.catsdogs.gametest.AnimaniaCatsDogsGameTests.class);
    }

    /** Replace vanilla companions at the world boundary while preserving tame state. */
    private static void replaceVanillaCompanion(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        net.minecraft.world.entity.Entity vanilla = event.getEntity();
        boolean dog = vanilla instanceof Wolf;
        boolean cat = vanilla instanceof Ocelot;
        if ((!dog || !replaceWolves()) && (!cat || !replaceOcelots())) return;
        boolean baby = vanilla instanceof net.minecraft.world.entity.AgeableMob ageable && ageable.isBaby();
        String femalePrefix = dog ? "female_" : "queen_";
        String malePrefix = dog ? "male_" : "tom_";
        String childPrefix = dog ? "puppy_" : "kitten_";
        java.util.List<String> candidates = ENTITIES.keySet().stream()
                .filter(id -> baby ? id.startsWith(childPrefix) : (id.startsWith(femalePrefix) || id.startsWith(malePrefix)))
                .toList();
        if (candidates.isEmpty()) return;
        String selected = candidates.get(event.getLevel().getRandom().nextInt(candidates.size()));
        EntityType<?> registered = ENTITIES.get(selected).get();
        if (!(registered.create(event.getLevel()) instanceof AnimaniaAnimalEntity replacement)) return;
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        replacement.setUUID(vanilla.getUUID());
        replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        if (baby) replacement.setAge(-com.animania.common.config.AnimaniaConfig.BABY_GROWTH_TICKS.get());
        else replacement.setAge(0);
        if (dog && ((Wolf) vanilla).isTame()) {
            replacement.setTamed(true);
            replacement.setOwnerUUID(((Wolf) vanilla).getOwnerUUID());
            replacement.setSitting(((Wolf) vanilla).isOrderedToSit());
        }
        replacement.setPersistenceRequired();
        event.getLevel().addFreshEntity(replacement);
        event.setCanceled(true);
    }

    private static boolean replaceWolves() {
        try { return CatsDogsConfig.REPLACE_VANILLA_WOLVES.get(); }
        catch (IllegalStateException ignored) { return true; }
    }

    private static boolean replaceOcelots() {
        try { return CatsDogsConfig.REPLACE_VANILLA_OCELOTS.get(); }
        catch (IllegalStateException ignored) { return true; }
    }

    private static AnimalGender gender(String id) {
        if (id.startsWith("kitten_") || id.startsWith("puppy_")) return AnimalGender.CHILD;
        if (id.startsWith("queen_") || id.startsWith("female_")) return AnimalGender.FEMALE;
        return AnimalGender.MALE;
    }

    private static String family(String id) {
        int underscore = id.indexOf('_');
        return underscore > 0 ? id.substring(underscore + 1) : id;
    }

    private static float sizeFor(String id, boolean width) {
        if (id.startsWith("kitten_") || id.startsWith("puppy_")) return width ? 0.35f : 0.45f;
        return width ? 0.75f : 0.9f;
    }
}
