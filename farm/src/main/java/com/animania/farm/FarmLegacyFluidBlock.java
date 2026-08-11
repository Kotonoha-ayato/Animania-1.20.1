package com.animania.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/** Modern equivalent of the legacy milk/honey blocks' collision behavior. */
public final class FarmLegacyFluidBlock extends LiquidBlock {
    private final boolean honey;

    public FarmLegacyFluidBlock(RegistryObject<FlowingFluid> source, BlockBehaviour.Properties properties, boolean honey) {
        super(source, properties);
        this.honey = honey;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        Vec3 flow = state.getFluidState().getFlow(level, pos);
        double divisor = honey ? 2000.0D : 1000.0D;
        entity.push(flow.x / divisor, flow.y / divisor, flow.z / divisor);
        if (honey && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1, 0, false, false));
        }
    }

    public boolean isHoney() { return honey; }
}
