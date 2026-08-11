package com.animania.common.block;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/** Legacy mud collision behaviour: movement is heavily damped on contact. */
public final class AnimaniaMudBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14.08, 16);
    public AnimaniaMudBlock(Properties properties) {
        super(properties);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (AnimaniaAnimalEntity animal : level.getEntitiesOfClass(AnimaniaAnimalEntity.class, new AABB(pos),
                candidate -> candidate.isMuddy() && candidate.isPlaying() && isFarmPig(candidate))) {
            for (int i = 0; i < 8; i++) {
                level.addParticle(particle,
                        animal.getX() + (random.nextFloat() - 0.5D) * animal.getBbWidth(),
                        animal.getBoundingBox().minY + 0.5D,
                        animal.getZ() + (random.nextFloat() - 0.5D) * animal.getBbWidth(),
                        4.0D * (random.nextFloat() - 0.5D),
                        0.5D,
                        (random.nextFloat() - 0.5D) * 4.0D);
            }
        }
    }

    private static boolean isFarmPig(AnimaniaAnimalEntity animal) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null || !id.getNamespace().equals("animania_farm")) return false;
        String path = id.getPath();
        return path.startsWith("sow_") || path.startsWith("boar_") || path.startsWith("piglet_");
    }
}
