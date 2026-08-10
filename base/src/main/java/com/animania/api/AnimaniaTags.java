package com.animania.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class AnimaniaTags {
    public static final TagKey<Item> ANIMAL_FEED = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
            new ResourceLocation("animania", "animal_feed"));
    public static final TagKey<Item> ANIMAL_DRINK = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
            new ResourceLocation("animania", "animal_drink"));
    public static final TagKey<Item> BREEDING_FOOD = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
            new ResourceLocation("animania", "breeding_food"));

    private AnimaniaTags() {
    }
}

