package com.animania.farm.gametest;

import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaVehicleEntity;
import com.animania.farm.AnimaniaFarm;
import com.animania.farm.FarmCheeseMoldBlockEntity;
import com.animania.farm.FarmConfig;
import com.animania.farm.FarmHiveBlockEntity;
import com.animania.farm.FarmCheeseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.animania.farm.FarmContent;
import com.animania.farm.FarmFluids;
import com.animania.farm.FarmMilkBottleItem;
import com.animania.farm.FarmHoneyJarItem;
import com.animania.farm.FarmBrownEggItem;
import com.animania.farm.FarmCarvingKnifeItem;
import com.animania.farm.FarmRidingCropItem;
import com.animania.common.item.AnimaniaEntityEggItem;

@GameTestHolder("animania_farm")
@PrefixGameTestTemplate(false)
public final class AnimaniaFarmGameTests {
    @GameTest(template = "empty")
    public static void allFarmEntitiesHaveRegistryObjects(GameTestHelper helper) {
        helper.assertTrue(AnimaniaFarm.ENTITIES.size() >= 100, "farm legacy entity registry is incomplete");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaCowReplacementRetainsWorldBoundarySemantics(GameTestHelper helper) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) {
            helper.fail("vanilla cow could not be constructed");
            return;
        }
        cow.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        helper.runAtTickTime(2, () -> {
            var entities = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                    new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D));
            helper.assertTrue(entities.stream().anyMatch(entity -> {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return id != null && id.getNamespace().equals(AnimaniaFarm.MOD_ID)
                        && (id.getPath().startsWith("cow_") || id.getPath().startsWith("bull_"));
            }), "vanilla cow was not replaced by a registered Animania cow");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Cow.class,
                    new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D)).isEmpty(),
                    "vanilla cow remained after replacement");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void animalCareBreedingAndPersistence(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> femaleType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cow_angus").get();
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> maleType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("bull_angus").get();
        AnimaniaAnimalEntity female = spawn(helper, femaleType, 0);
        AnimaniaAnimalEntity male = spawn(helper, maleType, 2);
        female.setGender(AnimalGender.FEMALE);
        male.setGender(AnimalGender.MALE);
        female.setHunger(10);
        female.setThirst(10);
        helper.assertTrue(female.feed(new ItemStack(Items.WHEAT)), "animal rejected valid feed");
        helper.assertTrue(female.getHunger() > 10, "feed did not restore hunger");
        helper.assertTrue(female.drink(new ItemStack(Items.WATER_BUCKET)), "animal rejected valid drink");
        helper.assertTrue(female.getThirst() == 100, "drink did not restore thirst");
        female.setInLove(null);
        male.setInLove(null);
        helper.assertTrue(female.canBreedWith(male), "paired legacy male/female IDs were not recognised as one species");
        female.spawnChildFromBreeding((ServerLevel) helper.getLevel(), male);
        helper.assertTrue(female.isPregnant(), "breeding did not enter the server-side pregnancy state");
        AgeableMob offspring = female.getBreedOffspring((ServerLevel) helper.getLevel(), male);
        helper.assertTrue(offspring != null && offspring.getType() == AnimaniaFarm.ENTITIES.get("calf_angus").get(),
                "breeding did not resolve the legacy calf entity type");
        CompoundTag saved = new CompoundTag();
        female.setVariantName("regression");
        female.setSterilized(false);
        female.addAdditionalSaveData(saved);
        female.setVariantName("mutated");
        female.readAdditionalSaveData(saved);
        helper.assertTrue("regression".equals(female.getVariantName()), "entity NBT did not restore variant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pullableVehicleHasInventoryAndPassengerPath(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaVehicleEntity> type = (EntityType<? extends AnimaniaVehicleEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cart").get();
        AnimaniaVehicleEntity vehicle = type.create(helper.getLevel());
        if (vehicle == null) {
            helper.fail("cart entity could not be constructed");
            return;
        }
        vehicle.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(vehicle);
        vehicle.setItem(0, new ItemStack(Items.WHEAT, 3));
        helper.assertTrue(vehicle.getItem(0).getCount() == 3, "vehicle inventory did not accept cargo");
        CompoundTag saved = new CompoundTag();
        vehicle.addAdditionalSaveData(saved);
        vehicle.setItem(0, ItemStack.EMPTY);
        vehicle.readAdditionalSaveData(saved);
        helper.assertTrue(vehicle.getItem(0).getCount() == 3, "vehicle cargo was not serialized");
        helper.assertTrue(vehicle.boost(), "vehicle did not accept a riding-crop boost");
        helper.assertTrue(!vehicle.boost(), "vehicle accepted a duplicate boost before cooldown");

        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> horseType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("mare_draft").get();
        AnimaniaAnimalEntity horse = horseType.create(helper.getLevel());
        helper.assertTrue(horse != null, "draft horse could not be constructed for hitch test");
        if (horse == null) return;
        horse.moveTo(helper.absolutePos(new BlockPos(0, 1, 3)), 0.0F, 0.0F);
        horse.setAge(0);
        helper.getLevel().addFreshEntity(horse);
        helper.assertTrue(vehicle.tryAttachPuller(horse), "vehicle rejected an adult draft horse hitch");
        helper.assertTrue(vehicle.isPulled() && vehicle.getPuller() == horse, "vehicle hitch did not synchronize");
        CompoundTag hitch = new CompoundTag();
        vehicle.addAdditionalSaveData(hitch);
        helper.assertTrue(hitch.hasUUID("AnimaniaPuller"), "vehicle hitch UUID was not persisted");
        vehicle.detachPuller();
        helper.assertTrue(!vehicle.isPulled(), "vehicle hitch did not detach");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmSpecialItemsRetainLegacyUseSemantics(GameTestHelper helper) {
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("milk_bottle").get() instanceof FarmMilkBottleItem,
                "milk bottle is not the drinkable modern item");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("honey_jar").get() instanceof FarmHoneyJarItem,
                "honey jar is not the drinkable modern item");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("brown_egg").get() instanceof FarmBrownEggItem,
                "brown egg is not throwable");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("carving_knife").get() instanceof FarmCarvingKnifeItem,
                "carving knife did not retain durability semantics");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("riding_crop").get() instanceof FarmRidingCropItem,
                "riding crop did not retain boost semantics");
        helper.assertTrue(FarmContent.BROWN_EGG_PROJECTILE.get().create(helper.getLevel()) instanceof com.animania.farm.FarmBrownEggProjectile,
                "brown egg projectile was not registered as a synchronized Forge entity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmLactationAndEggLayStatePersists(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> cowType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cow_angus").get();
        AnimaniaAnimalEntity cow = spawn(helper, cowType, 0);
        cow.setGender(AnimalGender.FEMALE);
        helper.assertTrue(!cow.isMilkReady(), "cow was milkable at spawn while the legacy default is disabled");
        cow.setMilkReady(true);
        CompoundTag cowTag = new CompoundTag();
        cow.addAdditionalSaveData(cowTag);
        cow.setMilkReady(false);
        cow.readAdditionalSaveData(cowTag);
        helper.assertTrue(cow.isMilkReady(), "lactation state did not survive entity NBT");

        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> henType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("hen_leghorn").get();
        AnimaniaAnimalEntity hen = spawn(helper, henType, 3);
        hen.setGender(AnimalGender.FEMALE);
        CompoundTag henTag = new CompoundTag();
        henTag.putInt("AnimaniaEggLayTicks", 1);
        hen.readAdditionalSaveData(henTag);
        hen.setGender(AnimalGender.FEMALE);
        hen.setAge(0);
        helper.assertTrue(hen.tryLayFarmEgg(true), "enabled hen egg laying did not produce an egg");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new AABB(helper.absolutePos(new BlockPos(3, 1, 0))).inflate(2.0D)).isEmpty(),
                "hen egg was not spawned as a server item entity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void randomEggsAreRealServerItems(GameTestHelper helper) {
        for (String id : new String[]{"entity_egg_cow_random", "entity_egg_chicken_random", "entity_egg_pig_random",
                "entity_egg_goat_random", "entity_egg_sheep_random"}) {
            helper.assertTrue(FarmContent.ITEM_ENTRIES.get(id).get() instanceof AnimaniaEntityEggItem,
                    id + " is an inert placeholder instead of an entity egg");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void cheeseMoldAcceptsModernMilkFluid(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(pos, FarmContent.CHEESE_MOLD.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof FarmCheeseMoldBlockEntity mold)) {
            helper.fail("fluid cheese mold did not create its block entity");
            return;
        }
        int filled = mold.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null)
                .map(handler -> handler.fill(new net.minecraftforge.fluids.FluidStack(FarmFluids.MILK_HOLSTEIN.source.get(), 1000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)).orElse(0);
        helper.assertTrue(filled == 1000, "cheese mold rejected registered Holstein milk fluid");
        int originalMaturity = FarmConfig.CHEESE_MATURITY_TIME.get();
        FarmConfig.CHEESE_MATURITY_TIME.set(200);
        helper.runAtTickTime(205, () -> {
            FarmConfig.CHEESE_MATURITY_TIME.set(originalMaturity);
            helper.assertTrue(mold.getItem(0).is(FarmContent.ITEM_ENTRIES.get("holstein_cheese_wheel").get()),
                    "Holstein milk did not produce the matching cheese wheel");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void draftHorseSaddleAndBoostStatePersists(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("mare_draft").get();
        AnimaniaAnimalEntity horse = type.create(helper.getLevel());
        helper.assertTrue(horse != null, "draft horse entity could not be constructed");
        if (horse == null) return;
        horse.setAge(0);
        horse.setSaddled(true);
        helper.assertTrue(horse.isSaddled(), "horse saddle state did not synchronize");
        helper.assertTrue(horse.boost(), "saddled horse rejected a riding-crop boost");
        CompoundTag tag = new CompoundTag();
        horse.addAdditionalSaveData(tag);
        horse.setSaddled(false);
        horse.readAdditionalSaveData(tag);
        helper.assertTrue(horse.isSaddled(), "horse saddle state did not persist through NBT");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void childGrowsIntoAdultRegistryType(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> childType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("calf_angus").get();
        AnimaniaAnimalEntity child = childType.create(helper.getLevel());
        if (child == null) {
            helper.fail("calf entity could not be constructed");
            return;
        }
        child.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        child.setAge(-20);
        helper.getLevel().addFreshEntity(child);
        helper.runAtTickTime(25, () -> {
            var grown = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                    new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D));
            helper.assertTrue(grown.stream().anyMatch(entity -> {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return id != null && ("cow_angus".equals(id.getPath()) || "bull_angus".equals(id.getPath()))
                        && entity.isAdult();
            }), "calf did not become an adult cow/bull registry entity");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void farmFluidsAndCheeseMoldProcess(GameTestHelper helper) {
        helper.assertTrue(FarmFluids.ALL.size() == 6, "legacy milk and honey fluids were not all registered");
        FarmFluids.ALL.values().forEach(fluid -> {
            helper.assertTrue(fluid.source.isPresent() && fluid.flowing.isPresent(), "missing source/flowing fluid " + fluid.id);
            helper.assertTrue(fluid.block.isPresent() && fluid.bucket.isPresent(), "missing fluid block/bucket " + fluid.id);
        });
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(pos, FarmContent.CHEESE_MOLD.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof FarmCheeseMoldBlockEntity mold)) {
            helper.fail("farm cheese mold did not create its block entity");
            return;
        }
        mold.setItem(0, new ItemStack(FarmContent.ITEM_ENTRIES.get("milk_bottle").get()));
        int originalMaturity = FarmConfig.CHEESE_MATURITY_TIME.get();
        FarmConfig.CHEESE_MATURITY_TIME.set(200);
        helper.runAtTickTime(205, () -> {
            FarmConfig.CHEESE_MATURITY_TIME.set(originalMaturity);
            helper.assertTrue(mold.getItem(0).is(FarmContent.ITEM_ENTRIES.get("friesian_cheese_wedge").get()),
                    "milk bottle did not process into legacy friesian cheese");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void hiveFluidAndCheeseBlockState(GameTestHelper helper) {
        BlockPos hivePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(hivePos, FarmContent.HIVE.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(hivePos) instanceof FarmHiveBlockEntity, "hive block entity was not registered");
        FarmHiveBlockEntity hive = (FarmHiveBlockEntity) helper.getLevel().getBlockEntity(hivePos);
        helper.assertTrue(hive.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).isPresent(), "hive fluid capability missing");
        helper.assertTrue(hive.honeyTank().fill(new net.minecraftforge.fluids.FluidStack(FarmFluids.ALL.get("animania_honey").source.get(), 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "hive rejected animania honey");
        BlockPos cheesePos = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.getLevel().setBlock(cheesePos, FarmContent.CHEESE_FRIESIAN.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(cheesePos).getValue(FarmCheeseBlock.BITES) == 0, "cheese did not start at zero bites");
        helper.assertTrue(FarmContent.CHEESE_FRIESIAN.get().getAnalogOutputSignal(helper.getLevel().getBlockState(cheesePos), helper.getLevel(), cheesePos) == 4,
                "cheese comparator level is incorrect");
        helper.succeed();
    }

    private static AnimaniaAnimalEntity spawn(GameTestHelper helper, EntityType<? extends AnimaniaAnimalEntity> type, int x) {
        AnimaniaAnimalEntity entity = type.create(helper.getLevel());
        if (entity == null) throw new IllegalStateException("registered farm entity could not be constructed");
        entity.moveTo(helper.absolutePos(new BlockPos(x, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(entity);
        entity.setAge(0);
        return entity;
    }

    private AnimaniaFarmGameTests() { }
}
