package com.animania.catsdogs;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Native pet facility block retaining the legacy shared pet_prop block entity.
 */
public final class CatsDogsPetFacilityBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final String id;

    public CatsDogsPetFacilityBlock(String id, BlockBehaviour.Properties properties) {
        super(properties);
        this.id = id;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof AnimaniaAnimalEntity animal && isPet(animal)) {
            if (id.startsWith("cat_bed") || id.equals("cat_tower") || id.equals("dog_house") || id.equals("dog_pillow")) {
                animal.setSleeping(true);
                animal.setPlaying(false);
            } else if (id.equals("litter_box") && isCat(animal)) {
                animal.setThirst(100);
                animal.setPlaying(false);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.animania.pet_facility", id), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CatsDogsPetFacilityBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    private static boolean isPet(AnimaniaAnimalEntity animal) {
        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id != null && "animania_catsdogs".equals(id.getNamespace());
    }

    private static boolean isCat(AnimaniaAnimalEntity animal) {
        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id != null && (id.getPath().startsWith("queen_") || id.getPath().startsWith("tom_") || id.getPath().startsWith("kitten_"));
    }
}
