package com.animania.farm;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Seven-state replacement for the metadata variants of the 1.12 wool block. */
public final class FarmWoolBlock extends Block {
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public FarmWoolBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.DORSET_BROWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    public enum Variant implements StringRepresentable {
        DORSET_BROWN("dorset_brown"), FRIESIAN_BLACK("friesian_black"),
        FRIESIAN_BROWN("friesian_brown"), JACOB("jacob"), MERINO_BROWN("merino_brown"),
        MERINO_WHITE("merino_white"), SUFFOLK_BROWN("suffolk_brown");

        private final String serializedName;

        Variant(String serializedName) { this.serializedName = serializedName; }
        @Override public String getSerializedName() { return serializedName; }
    }
}
