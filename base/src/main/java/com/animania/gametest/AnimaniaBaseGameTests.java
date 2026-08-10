package com.animania.gametest;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.AnimaniaFluids;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.animania.common.AnimaniaItems;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.item.AnimaniaEntityEggItem;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Smoke GameTests run by the dedicated Forge GameTest server. */
@GameTestHolder("animania")
@PrefixGameTestTemplate(false)
public final class AnimaniaBaseGameTests {
    private AnimaniaBaseGameTests() {
    }

    @GameTest(template = "empty")
    public static void apiContractLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyBaseItemsAndRandomEggRetainRegistrySemantics(GameTestHelper helper) {
        helper.assertTrue(AnimaniaItems.LEGACY_MANUAL.get() instanceof com.animania.common.item.ManualItem,
                "animania_manual did not retain the native manual item");
        helper.assertTrue(AnimaniaItems.LEGACY_SLOP_BUCKET.get() instanceof net.minecraft.world.item.BucketItem,
                "bucket_slop did not retain a real Forge fluid bucket");
        helper.assertTrue(AnimaniaItems.ENTITY_EGG_RANDOM.get() instanceof AnimaniaEntityEggItem,
                "entity_egg_random was not registered as a server-side egg");
        helper.assertTrue(AnimaniaItems.ENTITY_EGG_RANDOM.get().getMaxStackSize() == 64,
                "legacy entity eggs no longer use the 64-item stack size");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverAuthoritySmoke(GameTestHelper helper) {
        if (helper.getLevel().isClientSide()) {
            helper.fail("Base GameTests must run on a server level");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void storageCapabilitiesPersist(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage)) {
            helper.fail("trough did not create its storage block entity");
            return;
        }
        var items = storage.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElseThrow();
        items.insertItem(0, new ItemStack(Items.WHEAT, 4), false);
        helper.assertTrue(storage.getItem(0).getCount() == 4, "item capability did not sync to container");
        items.extractItem(0, 2, false);
        helper.assertTrue(storage.getItem(0).getCount() == 2, "capability extraction did not sync to container");
        var fluids = storage.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        int filled = fluids.fill(new FluidStack(AnimaniaFluids.SOURCE_SLOP.get(), 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "fluid capability rejected registered slop fluid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughFoodConfigUsesModernRegistryMatching(GameTestHelper helper) {
        helper.assertTrue(AnimaniaConfig.matchesTroughFood(new ItemStack(Items.WHEAT)),
                "default troughFood did not accept minecraft:wheat");
        helper.assertFalse(AnimaniaConfig.matchesTroughFood(new ItemStack(Items.DIRT)),
                "troughFood accepted an unconfigured item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saltLickCareAndDurability(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 0));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.SALT_LICK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof AnimaniaSaltLickBlockEntity lick)) {
            helper.fail("salt lick did not create its block entity");
            return;
        }
        int before = lick.usesLeft();
        lick.serverTick();
        helper.assertTrue(before == lick.usesLeft(), "unused salt lick changed durability");
        helper.succeed();
    }
}
