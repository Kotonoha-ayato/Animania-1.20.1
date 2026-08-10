package com.animania.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four-bite cheese wheel with server-authoritative eating and comparator state. */
public final class FarmCheeseBlock extends Block {
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 3);
    private static final VoxelShape[] SHAPES = {
            Block.box(1, 0, 1, 15, 8, 15), Block.box(1, 0, 1, 15, 8, 15),
            Block.box(1, 0, 1, 15, 8, 8), Block.box(1, 0, 1, 8, 8, 8)
    };
    private final String family;

    public FarmCheeseBlock(String family, BlockBehaviour.Properties properties) {
        super(properties);
        this.family = family;
        registerDefaultState(defaultBlockState().setValue(BITES, 0));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BITES)];
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.canEat(false)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int bites = state.getValue(BITES);
        player.getFoodData().eat(2, 1.2F);
        if (bites >= 3) level.removeBlock(pos, false);
        else level.setBlock(pos, state.setValue(BITES, bites + 1), 3);
        player.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 0.6F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return 4 - state.getValue(BITES);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    public String family() { return family; }
}
