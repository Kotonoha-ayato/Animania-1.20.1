package com.animania.common;

import com.animania.Animania;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.entity.EntityType;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.animania.common.item.ManualItem;
import com.animania.common.item.AnimaniaFoodItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class AnimaniaItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Animania.MOD_ID);
    public static final RegistryObject<Item> MANUAL = ITEMS.register("manual",
            () -> new ManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** Exact 1.12 registry spelling retained for old recipes and commands. */
    public static final RegistryObject<Item> LEGACY_MANUAL = ITEMS.register("animania_manual",
            () -> new ManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> HAY = ITEMS.register("hay",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SALT = ITEMS.register("salt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CHEESE = ITEMS.register("cheese",
            () -> new AnimaniaFoodItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationMod(0.5f).build())));
    public static final RegistryObject<Item> WATER_BOTTLE = ITEMS.register("water_bottle",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SLOP_BUCKET = ITEMS.register("slop_bucket",
            () -> new BucketItem(AnimaniaFluids.SOURCE_SLOP,
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    /** 1.12 Forge generated this alternate bucket spelling in some worlds. */
    public static final RegistryObject<Item> LEGACY_SLOP_BUCKET = ITEMS.register("bucket_slop",
            () -> new BucketItem(AnimaniaFluids.SOURCE_SLOP,
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    /** Random all-Animania egg; addon entity types are discovered at use time. */
    public static final RegistryObject<Item> ENTITY_EGG_RANDOM = ITEMS.register("entity_egg_random",
            () -> new AnimaniaEntityEggItem(AnimaniaItems::allAnimalTypes, new Item.Properties(), true));

    @SuppressWarnings("unchecked")
    private static List<EntityType<? extends AnimaniaAnimalEntity>> allAnimalTypes() {
        List<EntityType<? extends AnimaniaAnimalEntity>> types = new ArrayList<>();
        ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
            String namespace = entry.getKey().location().getNamespace();
            String path = entry.getKey().location().getPath();
            if ((namespace.equals(Animania.MOD_ID) || namespace.startsWith("animania_"))
                    && !path.equals("cart") && !path.equals("wagon") && !path.equals("tiller")
                    && !path.startsWith("item_")) {
                types.add((EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) entry.getValue());
            }
        });
        return types;
    }

    private AnimaniaItems() {
    }
}
