package com.animania.extra;

import com.animania.api.AnimaniaApi;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(AnimaniaExtra.MOD_ID)
public final class AnimaniaExtra {
    public static final String MOD_ID = "animania_extra";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final Map<String, RegistryObject<EntityType<?>>> ENTITIES = new LinkedHashMap<>();

    static { ExtraLegacyIds.ALL.forEach(AnimaniaExtra::register); }

    private static void register(String id) {
        RegistryObject<EntityType<?>> registered = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.of(AnimaniaAnimalEntity::new, MobCategory.CREATURE)
                        .sized(sizeFor(id, true), sizeFor(id, false)).clientTrackingRange(8).updateInterval(3)
                        .build(MOD_ID + ":" + id));
        ENTITIES.put(id, registered);
        AnimaniaApi.registerSpecies(new SpeciesDefinition(new ResourceLocation(MOD_ID, id), family(id), gender(id), sizeFor(id, true), sizeFor(id, false), 20000));
    }

    public AnimaniaExtra() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
        ExtraContent.ITEMS.register(bus);
        ExtraContent.BLOCKS.register(bus);
        ExtraContent.BLOCK_ENTITIES.register(bus);
        ExtraTab.TABS.register(bus);
        AnimaniaApi.registerFoodMatcher(MOD_ID, (id, stack) -> ExtraConfig.matchesSpeciesFood(id, stack));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ExtraConfig.SPEC);
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerGameTests);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaExtra::replaceVanillaRabbit);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaExtraClient::onClientSetup));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaExtraClient::registerLayers));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        ENTITIES.values().forEach(type -> event.put((EntityType<? extends LivingEntity>) type.get(), AnimaniaAnimalEntity.createAttributes().build()));
    }

    private void spawnPlacements(SpawnPlacementRegisterEvent event) {
        // Forge may dispatch this event before the common config is loaded in a
        // GameTest/dev bootstrap. Use the default during that early window.
        if (!spawnsEnabled()) return;
        ENTITIES.forEach((id, type) -> {
            if (familySpawnsEnabled(id)) {
                event.register((EntityType<? extends AnimaniaAnimalEntity>) type.get(), SpawnPlacements.Type.ON_GROUND,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AnimaniaAnimalEntity::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
            }
        });
    }

    private static boolean spawnsEnabled() {
        try {
            return ExtraConfig.ENABLE_SPAWNS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static boolean familySpawnsEnabled(String id) {
        if (id.startsWith("buck_") || id.startsWith("doe_") || id.startsWith("kit_")) return configured(ExtraConfig.SPAWN_ANIMANIA_RABBITS);
        if (id.startsWith("peacock_") || id.startsWith("peahen_") || id.startsWith("peachick_")) return configured(ExtraConfig.SPAWN_ANIMANIA_PEACOCKS);
        if (id.equals("toad") || id.equals("frog") || id.equals("dartfrog")) return configured(ExtraConfig.SPAWN_ANIMANIA_AMPHIBIANS);
        if (id.equals("hamster") || id.startsWith("ferret_") || id.startsWith("hedgehog")) return configured(ExtraConfig.SPAWN_ANIMANIA_RODENTS);
        return true;
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static void replaceVanillaRabbit(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !configured(ExtraConfig.REPLACE_VANILLA_RABBITS)) return;
        Entity vanilla = event.getEntity();
        if (!(vanilla instanceof Rabbit rabbit)) return;
        boolean baby = rabbit.isBaby();
        String selected = ExtraLegacyIds.ALL.stream()
                .filter(id -> baby ? id.startsWith("kit_") : (id.startsWith("doe_") || id.startsWith("buck_")))
                .skip(event.getLevel().getRandom().nextInt(Math.max(1, (int) ExtraLegacyIds.ALL.stream()
                        .filter(id -> baby ? id.startsWith("kit_") : (id.startsWith("doe_") || id.startsWith("buck_"))).count())))
                .findFirst().orElse(null);
        if (selected == null) return;
        EntityType<?> registered = ENTITIES.get(selected).get();
        if (!(registered.create(event.getLevel()) instanceof AnimaniaAnimalEntity replacement)) return;
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        replacement.setUUID(vanilla.getUUID());
        replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        replacement.setPersistenceRequired();
        if (baby) replacement.setAge(-Math.max(1, com.animania.common.config.AnimaniaConfig.BABY_GROWTH_TICKS.get()));
        event.getLevel().addFreshEntity(replacement);
        event.setCanceled(true);
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.extra.gametest.AnimaniaExtraGameTests.class);
    }

    private static AnimalGender gender(String id) {
        if (id.startsWith("kit_") || id.startsWith("peachick_")) return AnimalGender.CHILD;
        if (id.startsWith("doe_") || id.startsWith("peahen_")) return AnimalGender.FEMALE;
        return AnimalGender.MALE;
    }

    private static String family(String id) {
        int underscore = id.indexOf('_');
        return underscore > 0 ? id.substring(underscore + 1) : id;
    }

    private static float sizeFor(String id, boolean width) {
        if (id.startsWith("kit_") || id.startsWith("peachick_")) return width ? 0.35f : 0.45f;
        if (id.equals("hamster")) return width ? 0.35f : 0.3f;
        return width ? 0.7f : 0.8f;
    }
}
