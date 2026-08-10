package com.animania.catsdogs;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.item.AnimaniaEntityEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatsDogsContent {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnimaniaCatsDogs.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AnimaniaCatsDogs.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AnimaniaCatsDogs.MOD_ID);
    public static final Map<String, RegistryObject<Item>> ITEM_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> BLOCK_ENTRIES = new LinkedHashMap<>();
    public static final List<String> ITEM_IDS = List.of("entity_egg_cat_random", "entity_egg_dog_random");
    public static final List<String> BLOCK_IDS = List.of("cat_bed_1", "cat_bed_2", "cat_tower", "dog_house", "dog_pillow", "litter_box");
    public static final RegistryObject<Block> PET_BOWL = BLOCKS.register("pet_bowl", () -> new CatsDogsPetBowlBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.2f).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<BlockEntityType<CatsDogsPetBowlBlockEntity>> PET_BOWL_BE = BLOCK_ENTITIES.register("pet_bowl",
            () -> BlockEntityType.Builder.of(CatsDogsPetBowlBlockEntity::new, PET_BOWL.get()).build(null));

    static {
        ITEM_IDS.forEach(id -> {
            if (id.startsWith("entity_egg_")) registerGenericItem(id);
            else ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
        });
        ITEM_ENTRIES.put("pet_bowl", ITEMS.register("pet_bowl", () -> new net.minecraft.world.item.BlockItem(PET_BOWL.get(), new Item.Properties())));
        CatsDogsLegacyIds.ALL.forEach(id -> registerGenericItem("entity_egg_" + id));
        List.of("cat_random", "dog_random").forEach(id -> registerEggItem(id, id));
        BLOCK_IDS.forEach(id -> {
            RegistryObject<Block> block = BLOCKS.register(id, () -> new CatsDogsPetFacilityBlock(id,
                    BlockBehaviour.Properties.of().mapColor(id.equals("litter_box") ? MapColor.SAND : MapColor.WOOD)
                            .strength(1.0f).sound(SoundType.WOOD).noOcclusion()));
            BLOCK_ENTRIES.put(id, block);
            ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties())));
        });
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
                    () -> eggCandidates(target), new Item.Properties(), true)));
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends AnimaniaAnimalEntity> animalType(String id) {
        var entry = AnimaniaCatsDogs.ENTITIES.get(id);
        return entry == null ? null : (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) entry.get();
    }

    private static List<EntityType<? extends AnimaniaAnimalEntity>> eggCandidates(String target) {
        if (!target.endsWith("_random")) {
            EntityType<? extends AnimaniaAnimalEntity> type = animalType(target);
            return type == null ? List.of() : List.of(type);
        }
        String family = target.substring(0, target.length() - "_random".length());
        return CatsDogsLegacyIds.ALL.stream()
                .filter(id -> family.equals("cat")
                        ? id.startsWith("queen_") || id.startsWith("tom_")
                        : family.equals("dog") && (id.startsWith("female_") || id.startsWith("male_")))
                .map(CatsDogsContent::animalType)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private CatsDogsContent() { }
}
