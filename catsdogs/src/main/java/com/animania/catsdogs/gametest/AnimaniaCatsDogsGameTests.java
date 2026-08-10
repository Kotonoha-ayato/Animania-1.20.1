package com.animania.catsdogs.gametest;

import com.animania.catsdogs.AnimaniaCatsDogs;
import com.animania.catsdogs.CatsDogsContent;
import com.animania.catsdogs.CatsDogsPetBowlBlockEntity;
import com.animania.catsdogs.CatsDogsPetBowlBlock;
import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.animania.common.AnimaniaItems;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("animania_catsdogs")
@PrefixGameTestTemplate(false)
public final class AnimaniaCatsDogsGameTests {
    @GameTest(template = "empty")
    public static void allPetEntitiesHaveRegistryObjects(GameTestHelper helper) {
        helper.assertTrue(AnimaniaCatsDogs.ENTITIES.size() >= 60, "cats/dogs legacy entity registry is incomplete");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void randomEggsAreRealServerItems(GameTestHelper helper) {
        helper.assertTrue(CatsDogsContent.ITEM_ENTRIES.get("entity_egg_cat_random").get() instanceof AnimaniaEntityEggItem,
                "cat random egg is an inert placeholder instead of an entity egg");
        helper.assertTrue(CatsDogsContent.ITEM_ENTRIES.get("entity_egg_dog_random").get() instanceof AnimaniaEntityEggItem,
                "dog random egg is an inert placeholder instead of an entity egg");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void petCareSterilizationAndSaveRoundTrip(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaCatsDogs.ENTITIES.values().iterator().next().get();
        AnimaniaAnimalEntity animal = type.create(helper.getLevel());
        if (animal == null) {
            helper.fail("registered pet entity could not be constructed");
            return;
        }
        animal.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(animal);
        animal.setAge(0);
        animal.setGender(AnimalGender.FEMALE);
        java.util.UUID owner = java.util.UUID.randomUUID();
        animal.setTamed(true);
        animal.setOwnerUUID(owner);
        animal.setSitting(true);
        helper.assertTrue(animal.isTamed() && animal.isSitting() && owner.equals(animal.getOwnerUUID()), "pet taming state was not synchronized");
        animal.setSterilized(true);
        helper.assertTrue(animal.isSterilized() && !animal.isPregnant(), "sterilization did not block pregnancy");
        helper.assertTrue(animal.play(new ItemStack(Items.STRING)), "pet rejected play item");
        CompoundTag tag = new CompoundTag();
        animal.setVariantName("pet_regression");
        animal.addAdditionalSaveData(tag);
        animal.setVariantName("mutated");
        animal.readAdditionalSaveData(tag);
        helper.assertTrue("pet_regression".equals(animal.getVariantName()), "pet entity NBT did not restore");
        helper.assertTrue(animal.isTamed() && owner.equals(animal.getOwnerUUID()) && animal.isSitting(), "pet taming state did not persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void petBowlFoodAndWaterCapabilities(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, CatsDogsContent.PET_BOWL.get().defaultBlockState(), 3);
        BlockEntity raw = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(CatsDogsContent.PET_BOWL.get()), "pet bowl block state was not placed: " + helper.getLevel().getBlockState(pos));
        helper.assertTrue(raw instanceof CatsDogsPetBowlBlockEntity, "pet bowl block entity was not registered: " + raw);
        CatsDogsPetBowlBlockEntity bowl = (CatsDogsPetBowlBlockEntity) raw;
        helper.assertTrue(bowl.tryInsertFood(new ItemStack(Items.COD)), "pet bowl rejected fish food");
        helper.assertTrue(bowl.getItem(0).getCount() == 1, "pet bowl food count is not one");
        helper.assertTrue(bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, null).map(handler ->
                handler.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE)
                        == FluidType.BUCKET_VOLUME).orElse(false), "pet bowl did not accept water capability");
        helper.assertTrue(!CatsDogsPetBowlBlock.isFoodItem(new ItemStack(AnimaniaItems.WATER_BOTTLE.get())),
                "water bottle was incorrectly treated as solid pet food");
        helper.assertTrue(bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, null).map(handler ->
                handler.fill(new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.SIMULATE) == 0)
                .orElse(false), "pet bowl accepted a non-water automation fluid");
        helper.assertTrue(bowl.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent(), "pet bowl item capability missing");
        helper.succeed();
    }

    private AnimaniaCatsDogsGameTests() { }
}
