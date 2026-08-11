package com.animania.extra;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.animania.common.item.AnimaniaFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtraContent {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnimaniaExtra.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AnimaniaExtra.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AnimaniaExtra.MOD_ID);
    public static final Map<String, RegistryObject<Item>> ITEM_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> BLOCK_ENTRIES = new LinkedHashMap<>();
    public static final List<String> ITEM_IDS = List.of("blue_peacock_feather", "white_peacock_feather", "charcoal_peacock_feather", "opal_peacock_feather",
            "peach_peacock_feather", "purple_peacock_feather", "taupe_peacock_feather", "hamster_food", "hamster_ball_clear", "hamster_ball_colored",
            "peacock_egg_blue", "peacock_egg_white", "raw_prime_rabbit", "cooked_prime_rabbit", "raw_frog_legs", "cooked_frog_legs",
            "raw_peacock", "cooked_peacock", "raw_prime_peacock", "cooked_prime_peacock",
            "entity_egg_peacock_random", "entity_egg_rabbit_random", "entity_egg_dart_frog");
    public static final RegistryObject<Block> HAMSTER_WHEEL = BLOCKS.register("hamster_wheel", () ->
            new ExtraHamsterWheelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD)));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<ExtraHamsterWheelBlockEntity>> HAMSTER_WHEEL_BE =
            BLOCK_ENTITIES.register("hamster_wheel", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(ExtraHamsterWheelBlockEntity::new, HAMSTER_WHEEL.get()).build(null));

    static { 
        ITEM_IDS.forEach(id -> {
            if (id.startsWith("entity_egg_")) registerGenericItem(id);
            else if (id.equals("hamster_ball_clear")) ITEM_ENTRIES.put(id,
                    ITEMS.register(id, () -> new AnimaniaHamsterBallItem(false)));
            else if (id.equals("hamster_ball_colored")) ITEM_ENTRIES.put(id,
                    ITEMS.register(id, () -> new AnimaniaHamsterBallItem(true)));
            else ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new AnimaniaFoodItem(foodOrPlain(id))));
        });
        ExtraLegacyIds.ALL.forEach(id -> registerGenericItem("entity_egg_" + id));
        BLOCK_ENTRIES.put("hamster_wheel", HAMSTER_WHEEL);
        ITEM_ENTRIES.put("hamster_wheel", ITEMS.register("hamster_wheel", () -> new net.minecraft.world.item.BlockItem(HAMSTER_WHEEL.get(), new Item.Properties())));
    }

    private static void registerGenericItem(String id) {
        if (!ITEM_ENTRIES.containsKey(id)) {
            if (id.startsWith("entity_egg_")) {
                String target = id.substring("entity_egg_".length());
                registerEggItem(id, target);
            } else {
                ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(16))));
            }
        }
    }

    private static void registerEggItem(String id, String target) {
        if (!ITEM_ENTRIES.containsKey(id)) {
            ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new AnimaniaEntityEggItem(
                    () -> eggCandidates(target), new Item.Properties(), true, target)));
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends AnimaniaAnimalEntity> animalType(String id) {
        var entry = AnimaniaExtra.ENTITIES.get(id);
        return entry == null ? null : (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) entry.get();
    }

    private static List<EntityType<? extends AnimaniaAnimalEntity>> eggCandidates(String target) {
        if (target.equals("dart_frog")) target = "dartfrog";
        if (!target.endsWith("_random")) {
            EntityType<? extends AnimaniaAnimalEntity> type = animalType(target);
            return type == null ? List.of() : List.of(type);
        }
        String family = target.substring(0, target.length() - "_random".length());
        return ExtraLegacyIds.ALL.stream()
                .filter(id -> switch (family) {
                    case "peacock" -> id.startsWith("peacock_") || id.startsWith("peahen_");
                    case "rabbit" -> id.startsWith("buck_") || id.startsWith("doe_");
                    default -> false;
                })
                .map(ExtraContent::animalType)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static Item.Properties foodOrPlain(String id) {
        net.minecraft.world.food.FoodProperties.Builder food = new net.minecraft.world.food.FoodProperties.Builder();
        com.animania.common.item.LegacyRawFoodProfile raw = com.animania.common.item.LegacyRawFoodProfile.forItemId(id);
        if (raw != null) {
            raw.apply(food);
        } else if (id.equals("cooked_prime_rabbit")) {
            food.nutrition(8).saturationMod(0.5F).effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 3), 1.0F);
        } else if (id.equals("cooked_frog_legs")) {
            food.nutrition(7).saturationMod(0.5F).effect(() -> new MobEffectInstance(MobEffects.JUMP, 1200, 2), 1.0F);
        } else if (id.equals("cooked_peacock")) {
            food.nutrition(6).saturationMod(0.5F).effect(() -> new MobEffectInstance(MobEffects.LUCK, 600, 0), 1.0F);
        } else if (id.equals("cooked_prime_peacock")) {
            food.nutrition(10).saturationMod(0.5F);
        } else {
            return new Item.Properties();
        }
        return new Item.Properties().food(food.alwaysEat().build());
    }

    private ExtraContent() { }
}
