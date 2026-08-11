package com.animania.farm;

import com.animania.common.block.AnimaniaContainerBlock;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

/** Cheese-mould block with a real storage block entity and automation path. */
public final class FarmCheeseMoldBlock extends AnimaniaContainerBlock {
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 10, 16);

    public FarmCheeseMoldBlock(BlockBehaviour.Properties properties) {
        super(properties, FarmCheeseMoldBlockEntity::new);
        registerDefaultState(defaultBlockState().setValue(VARIANT, Variant.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FarmCheeseMoldBlockEntity mold)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (isMilkBottle(held) && mold.getItem(0).isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            mold.setItem(0, new ItemStack(held.getItem()));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (!player.addItem(new ItemStack(Items.GLASS_BOTTLE))) player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
            return InteractionResult.CONSUME;
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held).orElse(null);
        if (handler != null) {
            FluidStack fluid = handler.getFluidInTank(0);
            if (isAnimaniaMilk(fluid)) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                boolean filled = mold.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).map(target -> {
                    int accepted = target.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE);
                    if (accepted < 1000) return false;
                    handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    target.fill(new FluidStack(fluid.getFluid(), 1000), IFluidHandler.FluidAction.EXECUTE);
                    replaceHeld(player, hand, handler.getContainer());
                    return true;
                }).orElse(false);
                if (!filled) return InteractionResult.PASS;
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.CONSUME;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    private static boolean isMilkBottle(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && AnimaniaFarm.MOD_ID.equals(id.getNamespace()) && id.getPath().equals("milk_bottle");
    }

    private static boolean isAnimaniaMilk(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id != null && AnimaniaFarm.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("milk_");
    }

    private static void replaceHeld(Player player, InteractionHand hand, ItemStack replacement) {
        if (player.getAbilities().instabuild) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.getCount() > 1) {
            held.shrink(1);
            if (!player.addItem(replacement)) player.drop(replacement, false);
        } else player.setItemInHand(hand, replacement);
    }

    public enum Variant implements StringRepresentable {
        EMPTY("empty"), HOLSTEIN_MILK("holstein_milk"), HOLSTEIN_CHEESE("holstein_cheese"),
        FRIESIAN_MILK("friesian_milk"), FRIESIAN_CHEESE("friesian_cheese"),
        GOAT_MILK("goat_milk"), GOAT_CHEESE("goat_cheese"),
        SHEEP_MILK("sheep_milk"), SHEEP_CHEESE("sheep_cheese"),
        WATER("water"), SALT("salt"), JERSEY_MILK("jersey_milk"), JERSEY_CHEESE("jersey_cheese");

        private final String serializedName;

        Variant(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
