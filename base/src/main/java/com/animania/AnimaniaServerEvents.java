package com.animania;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.animania.common.AnimaniaSeedPlacement;
import com.animania.common.config.AnimaniaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.animania.common.command.AnimaniaCommand;

/** Server-authoritative hooks shared by every addon. */
public final class AnimaniaServerEvents {
    private static final String RELEASES_URL = "https://github.com/kawer95/Animania-1.20.1/releases";

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AnimaniaCommand.register(event);
    }
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AnimaniaAnimalEntity animal) {
            animal.ensureValidState();
        }
    }

    @SubscribeEvent
    public void onSeedRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (AnimaniaSeedPlacement.variant(event.getItemStack().getItem()) == null) return;
        boolean shiftRequired;
        try { shiftRequired = AnimaniaConfig.SHIFT_SEED_PLACEMENT.get(); }
        catch (IllegalStateException ignored) { shiftRequired = false; }
        if (shiftRequired && !event.getEntity().isShiftKeyDown()) return;
        BlockPos target = event.getPos();
        if (!event.getLevel().getBlockState(target).canBeReplaced()) {
            if (event.getFace() == null) return;
            target = target.relative(event.getFace());
        }
        if (!AnimaniaSeedPlacement.place(event.getLevel(), target, event.getItemStack())) return;
        if (!event.getLevel().isClientSide) {
            if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
            event.getLevel().playSound(null, target, net.minecraft.sounds.SoundEvents.GRASS_PLACE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    @SubscribeEvent
    public void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getEntityType() != EntityType.SQUID || event.getSpawnType() != MobSpawnType.NATURAL) return;
        boolean allowFreshWater;
        try { allowFreshWater = AnimaniaConfig.SPAWN_FRESH_WATER_SQUIDS.get(); }
        catch (IllegalStateException ignored) { allowFreshWater = true; }
        if (!allowFreshWater && !event.getLevel().getBiome(event.getPos()).is(BiomeTags.IS_OCEAN)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        com.animania.network.AnimaniaNetwork.syncCarried(player);
        boolean enabled;
        try { enabled = AnimaniaConfig.SHOW_MOD_UPDATE_NOTIFICATION.get(); }
        catch (RuntimeException ignored) { enabled = true; }
        var container = ModList.get().getModContainerById(Animania.MOD_ID).orElse(null);
        if (container == null) return;
        VersionChecker.CheckResult result = VersionChecker.getResult(container.getModInfo());
        if (!shouldNotifyUpdate(enabled, result.status())) return;
        String url = result.url() == null || result.url().isBlank() ? RELEASES_URL : result.url();
        String target = result.target() == null ? "" : result.target().toString();
        Component download = Component.literal("[GitHub Releases]").withStyle(style -> style
                .withColor(ChatFormatting.GOLD).withUnderlined(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(RELEASES_URL)))
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        player.sendSystemMessage(Component.translatable("animania.updatetext.1")
                .append(" " + target + " ")
                .append(Component.translatable("animania.updatetext.2"))
                .append(" ").append(download));
    }

    public static boolean shouldNotifyUpdate(boolean enabled, VersionChecker.Status status) {
        return enabled && status != null && status.isOutdated();
    }
}
