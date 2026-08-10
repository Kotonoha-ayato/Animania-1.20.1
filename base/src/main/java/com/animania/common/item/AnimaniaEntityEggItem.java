package com.animania.common.item;

import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Modern replacement for the 1.12 entity-egg item.  Eggs are deliberately
 * server-side: the client only receives the spawned entity through the normal
 * entity tracking channel, so a double-click cannot create duplicate animals.
 */
public final class AnimaniaEntityEggItem extends Item {
    private final Supplier<List<EntityType<? extends AnimaniaAnimalEntity>>> candidates;

    public AnimaniaEntityEggItem(Supplier<EntityType<? extends AnimaniaAnimalEntity>> entity,
                                 Properties properties) {
        this(() -> List.of(entity.get()), properties, true);
    }

    public AnimaniaEntityEggItem(Supplier<List<EntityType<? extends AnimaniaAnimalEntity>>> candidates,
                                 Properties properties, boolean listSupplier) {
        // The 1.12 ItemEntityEgg used a 64 stack size.  Keep that gameplay
        // default while still allowing a caller to override it explicitly.
        super(properties.stacksTo(64));
        this.candidates = Objects.requireNonNull(candidates, "candidates");
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
        // A random egg picks a sex at spawn time.  Explicit child registrations
        // retain CHILD and explicit male/female registrations retain their ID.
        if (types.size() > 1 && entity.getGender() == AnimalGender.CHILD) {
            entity.setGender(ThreadLocalRandom.current().nextBoolean() ? AnimalGender.MALE : AnimalGender.FEMALE);
        }
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return true;
    }
}
