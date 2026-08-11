package com.animania.common.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Modern parser for registry-id[#count]{nbt} configuration values. */
public final class StringParser {
    private StringParser() { }

    public static Parsed parse(String raw) {
        if (raw == null) return new Parsed(null, 0, new CompoundTag());
        String value = raw.trim();
        CompoundTag tag = new CompoundTag();
        int tagStart = value.indexOf('{');
        if (tagStart >= 0) {
            tag = AnimaniaHelper.parseTag(value.substring(tagStart));
            value = value.substring(0, tagStart);
        }
        int count = 1;
        int countStart = value.indexOf('#');
        if (countStart >= 0) {
            try { count = Math.max(1, Integer.parseInt(value.substring(countStart + 1))); }
            catch (NumberFormatException ignored) { count = 1; }
            value = value.substring(0, countStart);
        }
        return new Parsed(ResourceLocation.tryParse(value), count, tag);
    }

    public static Item item(String raw) {
        ResourceLocation id = parse(raw).id();
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    public static ItemStack itemStack(String raw) {
        Parsed parsed = parse(raw);
        return AnimaniaHelper.itemStack(parsed.id(), parsed.count(), parsed.tag());
    }

    public record Parsed(ResourceLocation id, int count, CompoundTag tag) { }
}
