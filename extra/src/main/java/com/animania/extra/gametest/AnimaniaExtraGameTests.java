package com.animania.extra.gametest;

import com.animania.extra.AnimaniaExtra;
import com.animania.extra.ExtraContent;
import com.animania.extra.ExtraHamsterWheelBlockEntity;
import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.animania.common.item.AnimaniaEntityEggItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("animania_extra")
@PrefixGameTestTemplate(false)
public final class AnimaniaExtraGameTests {
    @GameTest(template = "empty")
    public static void allExtraEntitiesHaveRegistryObjects(GameTestHelper helper) {
        helper.assertTrue(AnimaniaExtra.ENTITIES.size() >= 50, "extra legacy entity registry is incomplete");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaRabbitReplacementRetainsWorldBoundarySemantics(GameTestHelper helper) {
        Rabbit rabbit = EntityType.RABBIT.create(helper.getLevel());
        if (rabbit == null) {
            helper.fail("vanilla rabbit could not be constructed");
            return;
        }
        rabbit.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(rabbit);
        helper.runAtTickTime(2, () -> {
            var entities = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D));
            helper.assertTrue(entities.stream().anyMatch(entity -> {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return id != null && id.getNamespace().equals(AnimaniaExtra.MOD_ID)
                        && (id.getPath().startsWith("doe_") || id.getPath().startsWith("buck_"));
            }), "vanilla rabbit was not replaced by a registered Animania rabbit");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Rabbit.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D)).isEmpty(),
                    "vanilla rabbit remained after replacement");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void randomEggsAreRealServerItems(GameTestHelper helper) {
        for (String id : new String[]{"entity_egg_peacock_random", "entity_egg_rabbit_random", "entity_egg_dart_frog"}) {
            helper.assertTrue(ExtraContent.ITEM_ENTRIES.get(id).get() instanceof AnimaniaEntityEggItem,
                    id + " is an inert placeholder instead of an entity egg");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraAnimalCareAndSaveRoundTrip(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaExtra.ENTITIES.values().iterator().next().get();
        AnimaniaAnimalEntity animal = type.create(helper.getLevel());
        if (animal == null) {
            helper.fail("registered extra entity could not be constructed");
            return;
        }
        animal.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(animal);
        animal.setAge(0);
        animal.setGender(AnimalGender.FEMALE);
        animal.setHunger(5);
        helper.assertTrue(animal.feed(new ItemStack(Items.WHEAT)), "extra animal rejected feed");
        helper.assertTrue(animal.getHunger() > 5, "extra animal hunger did not recover");
        helper.assertTrue(animal.play(new ItemStack(Items.STICK)), "extra animal rejected play item");
        helper.assertTrue(animal.isPlaying(), "play state was not synchronized");
        CompoundTag tag = new CompoundTag();
        animal.setVariantName("extra_regression");
        animal.addAdditionalSaveData(tag);
        animal.setVariantName("mutated");
        animal.readAdditionalSaveData(tag);
        helper.assertTrue("extra_regression".equals(animal.getVariantName()), "extra entity NBT did not restore");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hamsterWheelGeneratesForgeEnergy(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(pos, ExtraContent.HAMSTER_WHEEL.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof ExtraHamsterWheelBlockEntity wheel)) {
            helper.fail("hamster wheel did not create its block entity");
            return;
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaExtra.ENTITIES.get("hamster").get();
        AnimaniaAnimalEntity hamster = type.create(helper.getLevel());
        if (hamster == null) {
            helper.fail("hamster entity could not be constructed");
            return;
        }
        hamster.moveTo(helper.absolutePos(new BlockPos(2, 1, 0)), 0.0F, 0.0F);
        hamster.setHunger(100);
        helper.getLevel().addFreshEntity(hamster);
        wheel.serverTick();
        helper.assertTrue(wheel.isRunning(), "nearby hamster did not start the wheel");
        helper.assertTrue(wheel.energyStored() > 0, "hamster wheel did not generate Forge energy");
        helper.succeed();
    }

    private AnimaniaExtraGameTests() { }
}
