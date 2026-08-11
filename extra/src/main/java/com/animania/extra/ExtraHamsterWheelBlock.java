package com.animania.extra;

import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Functional hamster wheel block replacing the old legacy model tile entity. */
public final class ExtraHamsterWheelBlock extends AnimaniaContainerBlock {
    public ExtraHamsterWheelBlock(BlockBehaviour.Properties properties) {
        super(properties, ExtraHamsterWheelBlockEntity::new);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND
                && AnimaniaAnimalEntity.hasCarriedAnimal(player)
                && "animania_extra:hamster".equals(AnimaniaAnimalEntity.carriedAnimalType(player))
                && level.getBlockEntity(pos) instanceof ExtraHamsterWheelBlockEntity wheel
                && !wheel.isRunning()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("animania_extra", "hamster"));
            AnimaniaAnimalEntity hamster = type == null ? null : (AnimaniaAnimalEntity) type.create(level);
            if (hamster != null) {
                hamster.readAdditionalSaveData(AnimaniaAnimalEntity.carriedAnimalData(player));
                if (!hamster.isInBall() && hamster.getHunger() > 0) {
                    hamster.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            player.getYRot(), 0.0F);
                    hamster.setPersistenceRequired();
                    level.addFreshEntity(hamster);
                    AnimaniaAnimalEntity.clearCarriedAnimal(player);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, hamster.getSoundSource(), 1.0F, 1.0F);
                    player.swing(hand);
                    return InteractionResult.CONSUME;
                }
                hamster.discard();
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }
}
