package com.animania.common;

import com.animania.Animania;
import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.block.AnimaniaInvisibleBlock;
import com.animania.common.block.AnimaniaMudBlock;
import com.animania.common.block.AnimaniaSaltLickBlock;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.config.AnimaniaConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

public final class AnimaniaBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Animania.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Animania.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Animania.MOD_ID);

    public static final RegistryObject<Block> TROUGH = container("trough", MapColor.WOOD);
    public static final RegistryObject<Block> NEST = container("nest", MapColor.TERRACOTTA_BROWN);
    public static final RegistryObject<Block> CHEESE_MOLD = container("cheese_mold", MapColor.COLOR_YELLOW);
    public static final RegistryObject<Block> PET_BOWL = container("pet_bowl", MapColor.COLOR_RED);
    public static final RegistryObject<Block> SALT_LICK = saltLick();
    public static final RegistryObject<Block> MUD = simple("mud", MapColor.DIRT);
    public static final RegistryObject<Block> STRAW = simple("straw", MapColor.COLOR_YELLOW);
    public static final RegistryObject<Block> INVISIBLE_BLOCK = simple("invisiblock", MapColor.NONE);
    public static final RegistryObject<Block> SEEDS = simple("block_seeds", MapColor.PLANT);
    public static final RegistryObject<Block> HAMSTER_WHEEL = simple("hamster_wheel", MapColor.WOOD);

    public static final RegistryObject<BlockEntityType<AnimaniaStorageBlockEntity>> TROUGH_BE = blockEntity("trough", TROUGH, TroughEntity::new);
    public static final RegistryObject<BlockEntityType<AnimaniaStorageBlockEntity>> NEST_BE = blockEntity("nest", NEST, NestEntity::new);
    public static final RegistryObject<BlockEntityType<AnimaniaStorageBlockEntity>> CHEESE_MOLD_BE = blockEntity("cheese_mold", CHEESE_MOLD, CheeseMoldEntity::new);
    public static final RegistryObject<BlockEntityType<AnimaniaStorageBlockEntity>> PET_BOWL_BE = blockEntity("pet_bowl", PET_BOWL, PetBowlEntity::new);
    public static final RegistryObject<BlockEntityType<AnimaniaSaltLickBlockEntity>> SALT_LICK_BE = BLOCK_ENTITIES.register("salt_lick",
            () -> BlockEntityType.Builder.of(AnimaniaSaltLickBlockEntity::new, SALT_LICK.get()).build(null));

    private static RegistryObject<Block> simple(String name, MapColor color) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(color).strength(1.0f).sound(SoundType.WOOD);
            if (name.equals("mud")) return new AnimaniaMudBlock(properties.friction(0.6f));
            if (name.equals("invisiblock")) return new AnimaniaInvisibleBlock(properties);
            return new Block(properties);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> saltLick() {
        RegistryObject<Block> block = BLOCKS.register("salt_lick", () -> new AnimaniaSaltLickBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.2f).sound(SoundType.STONE)));
        ITEMS.register("salt_lick", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> container(String name, MapColor color) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new AnimaniaContainerBlock(BlockBehaviour.Properties.of().mapColor(color).strength(1.2f).sound(SoundType.WOOD),
                (pos, state) -> switch (name) {
                    case "trough" -> new TroughEntity(pos, state);
                    case "nest" -> new NestEntity(pos, state);
                    case "cheese_mold" -> new CheeseMoldEntity(pos, state);
                    case "pet_bowl" -> new PetBowlEntity(pos, state);
                    default -> throw new IllegalStateException("Unknown Animania container: " + name);
                }));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<BlockEntityType<AnimaniaStorageBlockEntity>> blockEntity(String name, RegistryObject<Block> block,
                                                                                            BlockEntityType.BlockEntitySupplier<AnimaniaStorageBlockEntity> factory) {
        return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static final class TroughEntity extends AnimaniaStorageBlockEntity {
        public TroughEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(TROUGH_BE.get(), pos, state);
        }
    }

    public static final class NestEntity extends AnimaniaStorageBlockEntity {
        private int layingTicks;

        public NestEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(NEST_BE.get(), pos, state);
        }

        @Override
        public void serverTick() {
            if (++layingTicks < AnimaniaConfig.LAID_TIMER.get()) return;
            layingTicks = 0;
            if (!getItem(0).isEmpty()) return;
            net.minecraft.world.item.Item egg = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_farm", "brown_egg"));
            if (egg != null) setItem(0, new ItemStack(egg));
        }
    }

    public static final class CheeseMoldEntity extends AnimaniaStorageBlockEntity {
        private int processTicks;

        public CheeseMoldEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(CHEESE_MOLD_BE.get(), pos, state);
        }

        @Override
        public void serverTick() {
            ItemStack input = getItem(0);
            net.minecraft.world.item.Item milk = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_farm", "milk_bottle"));
            net.minecraft.world.item.Item cheese = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_farm", "friesian_cheese_wedge"));
            if (milk == null || cheese == null || !input.is(milk)) {
                processTicks = 0;
                return;
            }
            if (++processTicks >= 200) {
                processTicks = 0;
                setItem(0, new ItemStack(cheese));
            }
        }
    }

    public static final class PetBowlEntity extends AnimaniaStorageBlockEntity {
        public PetBowlEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(PET_BOWL_BE.get(), pos, state);
        }
    }

    private AnimaniaBlocks() {
    }
}
