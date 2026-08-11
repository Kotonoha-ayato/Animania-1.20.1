package com.animania.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four-bite cheese wheel with server-authoritative eating and comparator state. */
public final class FarmCheeseBlock extends Block {
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 3);
    private static final VoxelShape[] SHAPES = {
            Block.box(1, 0, 1, 15, 8, 15),
            Shapes.or(Block.box(1, 0, 1, 8, 8, 8), Block.box(8, 0, 1, 15, 8, 8),
                    Block.box(1, 0, 8, 8, 8, 15)),
            Block.box(1, 0, 1, 15, 8, 8),
            Block.box(1, 0, 1, 8, 8, 8)
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
        if (configuredBonusEffects()) {
            switch (family) {
                case "friesian" -> player.addEffect(new MobEffectInstance(MobEffects.HEAL, 6, 2, false, false));
                case "goat" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0, false, false));
                case "sheep" -> player.addEffect(new MobEffectInstance(MobEffects.HEAL, 10, 0, false, false));
                default -> player.addEffect(new MobEffectInstance(MobEffects.HEAL, 12, 2, false, false));
            }
        }
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

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    private static boolean configuredBonusEffects() {
        try {
            return com.animania.common.config.AnimaniaConfig.FOODS_GIVE_BONUS_EFFECTS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    public String family() { return family; }
}
