package com.animania.common.item;

import com.animania.api.data.AnimalGender;
import com.animania.common.AnimaniaSounds;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Modern replacement for the 1.12 entity-egg item.  Eggs are deliberately
 * server-side: the client only receives the spawned entity through the normal
 * entity tracking channel, so a double-click cannot create duplicate animals.
 */
public final class AnimaniaEntityEggItem extends Item {
    private final Supplier<List<EntityType<? extends AnimaniaAnimalEntity>>> candidates;
    private final String legacyEntityId;

    public AnimaniaEntityEggItem(Supplier<EntityType<? extends AnimaniaAnimalEntity>> entity,
                                 Properties properties) {
        this(() -> List.of(entity.get()), properties, true, null);
    }

    public AnimaniaEntityEggItem(Supplier<List<EntityType<? extends AnimaniaAnimalEntity>>> candidates,
                                 Properties properties, boolean listSupplier) {
        this(candidates, properties, listSupplier, null);
    }

    public AnimaniaEntityEggItem(Supplier<List<EntityType<? extends AnimaniaAnimalEntity>>> candidates,
                                 Properties properties, boolean listSupplier, String legacyEntityId) {
        // The 1.12 ItemEntityEgg used a 64 stack size.  Keep that gameplay
        // default while still allowing a caller to override it explicitly.
        super(properties.stacksTo(64));
        this.candidates = Objects.requireNonNull(candidates, "candidates");
        this.legacyEntityId = legacyEntityId;
    }

    public int tintColor(int tintIndex) {
        LegacyEggColors.Colors colors = LegacyEggColors.forEntity(legacyEntityId);
        if (colors == null) return 0xFFFFFFFF;
        return switch (tintIndex) {
            case 0 -> 0xFF000000 | colors.primary();
            case 1 -> 0xFF000000 | colors.secondary();
            default -> 0xFFFFFFFF;
        };
    }

    public String legacyEntityId() {
        return legacyEntityId;
    }

    /**
     * Creates the deterministic client-only preview used by the legacy fancy
     * egg renderer.  The live spawn path above remains random and server-side;
     * previews never mutate a level or consume an item.
     */
    public AnimaniaAnimalEntity createPreview(Level level) {
        List<EntityType<? extends AnimaniaAnimalEntity>> types = candidates.get();
        if (level == null || types == null || types.isEmpty()) return null;
        int index = legacyEntityId == null ? 0 : Math.floorMod(legacyEntityId.hashCode(), types.size());
        AnimaniaAnimalEntity preview = types.get(index).create(level);
        if (preview == null) return null;
        preview.setAge(-1);
        preview.setPos(0.0D, 0.0D, 0.0D);
        preview.setYRot(20.0F);
        preview.setYHeadRot(20.0F);
        preview.setYBodyRot(20.0F);
        return preview;
    }

    /**
     * Forge's native item-extension hook replaces the removed 1.12 baked-model
     * wrapper.  It is installed only when fancy eggs are enabled, so the
     * normal generated two-layer egg model remains the zero-overhead default.
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        boolean fancy;
        try {
            fancy = com.animania.common.config.AnimaniaConfig.FANCY_EGGS.get();
        } catch (RuntimeException ignored) {
            fancy = false;
        }
        if (fancy) {
            consumer.accept(new IClientItemExtensions() {
                private final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer =
                        new com.animania.client.render.AnimaniaEggItemRenderer(
                                net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                                net.minecraft.client.Minecraft.getInstance().getEntityModels());

                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return renderer;
                }
            });
        }
    }

    /** Registers the legacy server-side dispenser path for one Animania egg. */
    public static void registerDispenserBehavior(AnimaniaEntityEggItem egg) {
        DispenserBlock.registerBehavior(egg, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos target = source.getPos().relative(facing);
                egg.spawn(source.getLevel(), null, stack, target);
                return stack;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        return spawn(level, player, context.getItemInHand(), spawnPos) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(2.0D));
        BlockPos spawnPos = BlockPos.containing(target.x, target.y, target.z);
        return spawn(level, player, stack, spawnPos)
                ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private boolean spawn(Level level, Player player, ItemStack stack, BlockPos spawnPos) {
        List<EntityType<? extends AnimaniaAnimalEntity>> types = candidates.get();
        if (types == null || types.isEmpty()) return false;
        EntityType<? extends AnimaniaAnimalEntity> type = types.get(ThreadLocalRandom.current().nextInt(types.size()));
        AnimaniaAnimalEntity entity = type.create(level);
        if (entity == null) return false;
        entity.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                ThreadLocalRandom.current().nextFloat() * 360.0F, 0.0F);
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(entity.getYRot());
        if (stack.hasCustomHoverName()) entity.setCustomName(stack.getHoverName());
        // A random egg picks a sex at spawn time.  Explicit child registrations
        // retain CHILD and explicit male/female registrations retain their ID.
        if (types.size() > 1 && entity.getGender() == AnimalGender.CHILD) {
            entity.setGender(ThreadLocalRandom.current().nextBoolean() ? AnimalGender.MALE : AnimalGender.FEMALE);
        }
        entity.markInteracted();
        entity.setPersistenceRequired();
        if (!level.addFreshEntity(entity)) return false;
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        if (player != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), AnimaniaSounds.COMBO.get(),
                    SoundSource.PLAYERS, 0.8F,
                    (ThreadLocalRandom.current().nextFloat() - ThreadLocalRandom.current().nextFloat()) * 0.25F + 1.25F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.animania_entity_egg.desc1")
                .append(" ")
                .append(Component.translatable("item.animania_entity_egg.desc2")));
    }
}
