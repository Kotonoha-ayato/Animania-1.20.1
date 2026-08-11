package com.animania.common.block;

import com.animania.common.AnimaniaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Legacy floor-scattered straw and seed piles. */
public final class AnimaniaThinBlock extends Block {
    public enum Kind { STRAW, SEEDS }
    public enum SeedVariant implements net.minecraft.util.StringRepresentable {
        WHEAT("wheat", Items.WHEAT_SEEDS), PUMPKIN("pumpkin", Items.PUMPKIN_SEEDS),
        MELON("melon", Items.MELON_SEEDS), BEETROOT("beetroot", Items.BEETROOT_SEEDS);
        private final String name;
        private final net.minecraft.world.item.Item item;
        SeedVariant(String name, net.minecraft.world.item.Item item) { this.name = name; this.item = item; }
        @Override public String getSerializedName() { return name; }
        public ItemStack drop() { return new ItemStack(item); }
    }

    public static final EnumProperty<SeedVariant> VARIANT = EnumProperty.create("variant", SeedVariant.class);
    private static final VoxelShape STRAW_OUTLINE = Block.box(0, 0, 0, 16, 0.032, 16);
    private static final VoxelShape SEEDS_OUTLINE = Block.box(0, 0, 0, 16, 0.0032, 16);
    private final Kind kind;

    public AnimaniaThinBlock(Properties properties, Kind kind) {
        super(properties.noCollission().noOcclusion());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(VARIANT, SeedVariant.WHEAT));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.STRAW ? STRAW_OUTLINE : SEEDS_OUTLINE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        if (kind == Kind.STRAW && (below.is(this)
                || !below.isFaceSturdy(context.getLevel(), context.getClickedPos().below(), Direction.UP)
                || !below.canOcclude())) return null;
        return canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos())
                ? defaultBlockState() : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                            BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide || oldState.is(state.getBlock())) return;
        BlockParticleOption particle = new BlockParticleOption(
                ParticleTypes.BLOCK, AnimaniaBlocks.SEEDS.get().defaultBlockState());
        for (int i = 0; i < 10; i++) {
            double zOffset = level.getRandom().nextDouble() * 0.6D - 0.3D;
            level.addParticle(particle,
                    pos.getX() + 0.25D,
                    pos.getY() + level.getRandom().nextDouble() * 6.0D / 16.0D + 0.75D,
                    pos.getZ() + 0.25D + zOffset,
                    0.0D, 0.0D, 0.0D);
        }
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return kind == Kind.SEEDS ? state.getValue(VARIANT).drop() : super.getCloneItemStack(level, pos, state);
    }
    @Override public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) { return true; }
    @Override public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return kind == Kind.STRAW; }
    @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return kind == Kind.STRAW ? 60 : 0; }
    @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return kind == Kind.STRAW ? 30 : 0; }
}
