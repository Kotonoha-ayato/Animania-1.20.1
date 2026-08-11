package com.animania.farm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Preserves the legacy global egg toggle and Extra rodent egg protection without an addon hard dependency. */
public final class FarmEggThrowHandler {
    private FarmEggThrowHandler() {}

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!isEgg(event.getItemStack()) || !shouldCancelEggUse(event.getLevel(), event.getEntity())) return;
        event.getEntity().swing(event.getHand());
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    public static boolean shouldCancelEggUse(Level level, Player player) {
        if (!configuredAllowThrowing()) return true;
        AABB range = player.getBoundingBox().inflate(3.0D, 2.0D, 3.0D);
        return level.getEntities(player, range, FarmEggThrowHandler::isEggProtectingRodent).size() > 0;
    }

    public static boolean isEggProtectingRodent(Entity entity) {
        return isEggProtectingRodent(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    public static boolean isEggProtectingRodent(ResourceLocation id) {
        if (id == null || !"animania_extra".equals(id.getNamespace())) return false;
        return id.getPath().equals("ferret_white") || id.getPath().equals("ferret_grey")
                || id.getPath().equals("hedgehog");
    }

    private static boolean isEgg(ItemStack stack) {
        return stack.is(Items.EGG) || stack.is(FarmContent.ITEM_ENTRIES.get("brown_egg").get());
    }

    private static boolean configuredAllowThrowing() {
        try { return FarmConfig.ALLOW_EGG_THROWING.get(); }
        catch (RuntimeException ignored) { return false; }
    }
}
