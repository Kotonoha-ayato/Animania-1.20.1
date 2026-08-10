package com.animania.common.entity;

import com.animania.api.AnimaniaApi;
import com.animania.api.AnimaniaTags;
import com.animania.api.interfaces.IAnimaniaAnimal;
import com.animania.api.data.AnimalAge;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.AnimalSnapshot;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.advancement.FeedAnimalTrigger;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared server-authoritative animal implementation.  Addons register one
 * EntityType per legacy ID; this class carries the common state and behaviour
 * so variant and sex changes never require duplicated entity implementations.
 */
public class AnimaniaAnimalEntity extends Animal implements IAnimaniaAnimal {
    private static final EntityDataAccessor<Byte> GENDER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> THIRST = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PLAYING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PREGNANT = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STERILIZED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<java.util.UUID>> OWNER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MILK_READY = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private int pregnancyTicks;
    private int playingTicks;
    private int woolRegrowthTicks;
    private int boostTicks;
    private int starvationTicks;
    private int eggLayTicks;
    private boolean roosterCombatConfigured;
    /**
     * 1.12 deliberately kept naturally spawned animals passive until a player
     * interacted with them.  This is server state (not a render hint), so it
     * is persisted with the entity and never inferred from a client packet.
     */
    private boolean interacted;

    public AnimaniaAnimalEntity(EntityType<? extends AnimaniaAnimalEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0f);
        // Entity data is defined by the time the constructor returns.  Infer
        // the baseline sex from the legacy registration ID so natural spawns
        // are not all CHILD until their first save/reload.
        AnimalGender inferred = inferGender();
        this.setGender(inferred);
        // Child registry IDs represent the legacy calf/kid/etc. entities. A
        // newly created child must start as a baby even when it came from an
        // egg item or a command (vanilla EntityType instances default to age
        // zero). The server-side growth path below replaces it with the
        // matching adult registry ID when the age reaches zero.
        if (inferred == AnimalGender.CHILD) {
            this.setAge(-Math.max(1, AnimaniaConfig.BABY_GROWTH_TICKS.get()));
        }
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 10.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.22D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PanicGoal(this, 1.35D));
        goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(2, new TemptGoal(this, 1.15D, Ingredient.of(Items.WHEAT, Items.CARROT, Items.WHEAT_SEEDS, Items.HAY_BLOCK), false));
        goalSelector.addGoal(3, new FollowParentGoal(this, 1.1D));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        // Cats and dogs retain the legacy companion combat intent while
        // remaining server-authoritative and opt-out through the shared rule.
        if (attacksAllowed() && isCompanionAnimal()) {
            goalSelector.addGoal(4, new MeleeAttackGoal(this, isDogCompanion() ? 1.15D : 1.0D, true));
            if (isCatCompanion()) goalSelector.addGoal(5, new LeapAtTargetGoal(this, 0.4F));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, true,
                    target -> !isTamed()));
            if (isDogCompanion()) {
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Sheep.class, true,
                        target -> !isTamed()));
                targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Rabbit.class, true,
                        target -> !isTamed()));
            } else {
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Chicken.class, true,
                        target -> !isTamed()));
            }
        }
    }

    private boolean attacksAllowed() {
        try {
            return AnimaniaConfig.ANIMALS_CAN_ATTACK.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GENDER, (byte) AnimalGender.CHILD.ordinal());
        entityData.define(VARIANT, "default");
        entityData.define(HUNGER, 100);
        entityData.define(THIRST, 100);
        entityData.define(SLEEPING, false);
        entityData.define(PLAYING, false);
        entityData.define(PREGNANT, false);
        entityData.define(STERILIZED, false);
        entityData.define(SHEARED, false);
        entityData.define(TAMED, false);
        entityData.define(SITTING, false);
        entityData.define(OWNER, Optional.empty());
        entityData.define(SADDLED, false);
        entityData.define(MILK_READY, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (isSitting()) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        } else {
            followOwnerIfNeeded();
        }
        if (config(AnimaniaConfig.AMBIANCE_MODE, false)) {
            // Ambiance mode keeps the care meters full and disables all
            // starvation pressure while retaining the visible state fields.
            setHunger(100);
            setThirst(100);
            starvationTicks = 0;
        } else {
            if (tickCount % Math.max(20, config(AnimaniaConfig.HUNGER_INTERVAL, 2400)) == 0) setHunger(getHunger() - 1);
            if (tickCount % Math.max(20, config(AnimaniaConfig.THIRST_INTERVAL, 1800)) == 0) setThirst(getThirst() - 1);
            if (config(AnimaniaConfig.ANIMALS_STARVE, false) && (getHunger() <= 0 || getThirst() <= 0)) {
                if (++starvationTicks >= Math.max(20, config(AnimaniaConfig.STARVATION_TIMER, 400))) {
                    starvationTicks = 0;
                    hurt(level().damageSources().starve(), 1.0F);
                }
            } else {
                starvationTicks = 0;
            }
        }
        if (config(AnimaniaConfig.ANIMALS_SLEEP, true)) {
            setSleeping(level().isNight() && !isInLove() && getDeltaMovement().lengthSqr() < 0.01D);
        }
        if (playingTicks > 0 && --playingTicks == 0) setPlaying(false);
        if (boostTicks > 0) boostTicks--;
        if (config(AnimaniaConfig.ALLOW_TROUGH_AUTOMATION, true) && tickCount % 20 == 0) consumeNearbyFacility();
        if (isSheared() && tickCount % 20 == 0 && --woolRegrowthTicks <= 0) setSheared(false);
        if (isAdult() && config(AnimaniaConfig.BIRDS_DROP_FEATHERS, true) && tickCount > 0
                && tickCount % Math.max(20, config(AnimaniaConfig.FEATHER_TIMER, 12000)) == 0) produceFeather();
        if (isBaby() && tickCount % 20 == 0 && getAge() < 0) {
            int nextAge = Math.min(0, getAge() + 20);
            setAge(nextAge);
        }
        // AgeableMob also advances its age one tick at a time. If that
        // vanilla path reaches zero before the 20-tick Animania growth step,
        // still perform the registry-ID transition on the first adult tick.
        if (getAge() >= 0 && isChildRegistryId()) {
            growIntoAdultVariant();
        }
        if (isPregnant() && ++pregnancyTicks >= pregnancyDuration()) giveBirth();
    }

    private void followOwnerIfNeeded() {
        if (!isTamed() || getOwnerUUID() == null || tickCount % 20 != 0 || !(level() instanceof ServerLevel server)) return;
        Player owner = server.getPlayerByUUID(getOwnerUUID());
        if (owner == null || owner.level() != level()) return;
        double distance = distanceToSqr(owner);
        if (distance > 4.0D && distance < 256.0D) {
            getNavigation().moveTo(owner, 1.15D);
        } else if (distance >= 256.0D && AnimaniaConfig.TAMED_ANIMALS_TELEPORT.get()) {
            BlockPos target = owner.blockPosition().above();
            if (level().getBlockState(target).isAir()) {
                moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, getYRot(), getXRot());
            }
        }
    }

    private int pregnancyDuration() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return AnimaniaApi.species(id).map(SpeciesDefinition::gestationTicks).orElse(AnimaniaConfig.GESTATION_TICKS.get());
    }

    private void consumeNearbyFacility() {
        int range = Math.max(1, Math.min(8, config(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16)));
        BlockPos min = blockPosition().offset(-range, -1, -range);
        BlockPos max = blockPosition().offset(range, 1, range);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!(level().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage)) continue;
            for (int slot = 0; slot < storage.getContainerSize(); slot++) {
                ItemStack stack = storage.getItem(slot);
                if (stack.isEmpty()) continue;
                if (getHunger() < 100 && isAnimaniaFood(stack)
                        && AnimaniaConfig.matchesTroughFood(stack) && feed(stack)) {
                    stack = stack.copy();
                    if (config(AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING, true)) stack.shrink(1);
                    storage.setItem(slot, stack);
                    return;
                }
                if (getThirst() < 100 && (stack.is(AnimaniaTags.ANIMAL_DRINK) || stack.is(Items.WATER_BUCKET)
                        || stack.is(Items.POTION)) && drink(stack)) {
                    stack = stack.copy();
                    if (config(AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING, true)) stack.shrink(1);
                    if (stack.isEmpty() && level().getBlockEntity(pos) != null) {
                        if (storage.getItem(slot).is(Items.WATER_BUCKET)) stack = new ItemStack(Items.BUCKET);
                        else if (storage.getItem(slot).is(Items.POTION)
                                || storage.getItem(slot).is(com.animania.common.AnimaniaItems.WATER_BOTTLE.get())) stack = new ItemStack(Items.GLASS_BOTTLE);
                    }
                    storage.setItem(slot, stack);
                    return;
                }
            }
        }
    }

    private void giveBirth() {
        if (getGender() != AnimalGender.FEMALE) {
            setPregnant(false);
            pregnancyTicks = 0;
            return;
        }
        setPregnant(false);
        pregnancyTicks = 0;
        // A hungry/thirsty female can lose a pregnancy when the legacy rule is
        // enabled.  The decision is made only on the authoritative level.
        if ((getHunger() <= 0 || getThirst() <= 0)
                && config(AnimaniaConfig.ANIMAL_LOSS_CHANCE, 0.0D) > 0.0D
                && random.nextDouble() < config(AnimaniaConfig.ANIMAL_LOSS_CHANCE, 0.0D)) {
            return;
        }
        int births = 1;
        double multipleChance = config(AnimaniaConfig.BIRTH_MULTIPLE_CHANCE, 0.1D);
        while (births < 4 && multipleChance > 0.0D && random.nextDouble() < multipleChance) births++;
        for (int index = 0; index < births; index++) {
            AnimaniaAnimalEntity child = (AnimaniaAnimalEntity) getBreedOffspring((ServerLevel) level(), this);
            if (child == null) continue;
            child.setAge(-Math.max(1, config(AnimaniaConfig.BABY_GROWTH_TICKS, 24000)));
            child.setGender(AnimalGender.CHILD);
            child.moveTo(getX() + (index * 0.25D), getY(), getZ() + (index * 0.25D), getYRot(), 0.0f);
            ((ServerLevel) level()).addFreshEntity(child);
        }
        if (isMilkSpecies()) setMilkReady(true);
        setAge(6000);
    }

    /**
     * Replace a legacy child EntityType with its adult sex-specific type at
     * the end of the vanilla age countdown. Keeping the registry ID change is
     * important for loot tables, rendering, breeding compatibility, and
     * save/reload parity with the 1.12 child entities.
     */
    private void growIntoAdultVariant() {
        if (!(level() instanceof ServerLevel server)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return;
        AnimalGender adultGender = random.nextBoolean() ? AnimalGender.FEMALE : AnimalGender.MALE;
        String adultPrefix = adultPrefix(id.getPath(), adultGender);
        if (adultPrefix == null) return;
        String species = speciesKey(id.getPath());
        // A few legacy families use a different sex prefix than the generic
        // female/male pair. The prefix mapping above is namespace-agnostic;
        // resolving the actual registered type keeps addons independent.
        ResourceLocation adultId = new ResourceLocation(id.getNamespace(), adultPrefix + species);
        EntityType<?> rawType = ForgeRegistries.ENTITY_TYPES.getValue(adultId);
        if (!(rawType instanceof EntityType<?>)) return;
        @SuppressWarnings("unchecked") EntityType<? extends AnimaniaAnimalEntity> adultType =
                (EntityType<? extends AnimaniaAnimalEntity>) rawType;
        AnimaniaAnimalEntity adult = new AnimaniaAnimalEntity(adultType, server);
        adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        adult.setAge(0);
        adult.setGender(adultGender);
        adult.setVariantName(getVariantName());
        adult.setHunger(getHunger());
        adult.setThirst(getThirst());
        adult.setSterilized(isSterilized());
        adult.setTamed(isTamed());
        adult.setOwnerUUID(getOwnerUUID());
        adult.setSitting(isSitting());
        adult.setPersistenceRequired();
        if (isPassenger()) {
            stopRiding();
        }
        discard();
        server.addFreshEntity(adult);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isHorseAnimal() && !isBaby()) {
            if (stack.is(Items.SADDLE) && !isSaddled()) {
                if (!level().isClientSide) {
                    setSaddled(true);
                    interacted = true;
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level().playSound(null, blockPosition(), SoundEvents.HORSE_SADDLE, getSoundSource(), 0.8F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (stack.isEmpty() && isSaddled() && !player.isPassenger()) {
                if (!level().isClientSide) player.startRiding(this);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (isCompanionAnimal()) {
            if (stack.isEmpty() && isTamed() && ownerMatches(player) && !isSleeping()) {
                if (!level().isClientSide) {
                    setSitting(!isSitting());
                    getNavigation().stop();
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!isTamed() && canTameWith(stack)) {
                if (!level().isClientSide) {
                    setTamed(true);
                    setOwnerUUID(player.getUUID());
                    setSitting(false);
                    interacted = true;
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level().broadcastEntityEvent(this, (byte) 7);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (stack.is(AnimaniaTags.ANIMAL_DRINK) || stack.is(Items.WATER_BUCKET) || stack.is(Items.POTION)) {
            if (!level().isClientSide) drink(stack);
            if (!level().isClientSide && !player.getAbilities().instabuild
                    && config(AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING, true)) {
                if (stack.is(Items.WATER_BUCKET) || stack.is(com.animania.common.AnimaniaItems.WATER_BOTTLE.get())) {
                    stack.shrink(1);
                } else if (stack.getItem() instanceof PotionItem) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(AnimaniaTags.ANIMAL_FEED) || isAnimaniaFood(stack)) {
            if (!level().isClientSide) {
                ItemStack fedItem = stack.copyWithCount(1);
                feed(stack);
                if (player instanceof ServerPlayer serverPlayer) {
                    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(getType());
                    if (entityId != null) FeedAnimalTrigger.INSTANCE.trigger(serverPlayer, fedItem, entityId);
                }
            }
            if (!level().isClientSide && !player.getAbilities().instabuild
                    && config(AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING, true)) stack.shrink(1);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.STICK) || stack.is(Items.STRING)) {
            if (!level().isClientSide) play(stack);
            if (!level().isClientSide && !player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.BUCKET) && isMilkable()) {
            if (!level().isClientSide) {
                Item result = milkBucket();
                if (result != null) {
                    interacted = true;
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    if (!player.addItem(new ItemStack(result))) player.drop(new ItemStack(result), false);
                    level().playSound(null, blockPosition(), SoundEvents.COW_MILK, getSoundSource(), 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.SHEARS) && !isBaby()) {
            if (!level().isClientSide && isShearable() && !isSheared()) {
                interacted = true;
                setSheared(true);
                woolRegrowthTicks = Math.max(20, AnimaniaConfig.WOOL_REGROWTH_TIMER.get());
                spawnAtLocation(new ItemStack(Items.WHITE_WOOL, 1 + random.nextInt(3)));
                stack.hurtAndBreak(1, player, broken -> player.broadcastBreakEvent(hand));
                level().playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, getSoundSource(), 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (isHorseAnimal()) return passenger instanceof Player && isSaddled() && getPassengers().isEmpty();
        return super.canAddPassenger(passenger);
    }

    @Override
    public void travel(Vec3 input) {
        if (isHorseAnimal() && isSaddled() && getControllingPassenger() instanceof Player rider) {
            setYRot(rider.getYRot());
            yRotO = getYRot();
            float strafe = rider.xxa * 0.5F;
            float forward = rider.zza;
            if (forward < 0.0F) forward *= 0.25F;
            float speed = (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    * (boostTicks > 0 ? 2.0F : 1.0F);
            moveRelative(speed, new Vec3(strafe, input.y, forward));
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.91D, 0.98D, 0.91D));
            return;
        }
        super.travel(input);
    }

    /** Start a short horse boost used by the riding crop. */
    public boolean boost() {
        if (!isHorseAnimal() || boostTicks > 0) return false;
        boostTicks = 40 + random.nextInt(80);
        return true;
    }

    protected boolean isAnimaniaFood(ItemStack stack) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return (entityId != null && AnimaniaApi.matchesRegisteredFood(entityId, stack))
                || stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT) || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.HAY_BLOCK)
                || stack.is(AnimaniaTags.BREEDING_FOOD) || (isCompanionAnimal() && isCompanionFood(stack));
    }

    private static boolean isAnimaniaDrink(ItemStack stack) {
        return stack.is(AnimaniaTags.ANIMAL_DRINK) || stack.is(Items.WATER_BUCKET)
                || stack.is(Items.POTION) || stack.is(com.animania.common.AnimaniaItems.WATER_BOTTLE.get());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isAnimaniaFood(stack);
    }

    @Override
    public boolean isAdult() {
        return !isBaby() && getGender() != AnimalGender.CHILD;
    }

    @Override
    public void setInLove(Player player) {
        if (!level().isClientSide) interacted = true;
        super.setInLove(player);
    }

    @Override
    public boolean canMate(Animal other) {
        return other instanceof AnimaniaAnimalEntity mate && canBreedWith(mate) && super.canMate(other);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        if (!(partner instanceof AnimaniaAnimalEntity mate) || !canBreedWith(mate)) {
            super.spawnChildFromBreeding(level, partner);
            return;
        }
        AnimaniaAnimalEntity female = getGender() == AnimalGender.FEMALE ? this
                : mate.getGender() == AnimalGender.FEMALE ? mate : null;
        if (female == null || female.isPregnant() || female.isSterilized()) return;
        female.setPregnant(true);
        female.pregnancyTicks = 0;
        if (getGender() == AnimalGender.MALE && !config(AnimaniaConfig.MALES_MATE_MULTIPLE_FEMALES, false)) setAge(6000);
        if (mate.getGender() == AnimalGender.MALE && !config(AnimaniaConfig.MALES_MATE_MULTIPLE_FEMALES, false)) mate.setAge(6000);
        resetLove();
        mate.resetLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        @SuppressWarnings("unchecked") EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) childType(getType());
        AnimaniaAnimalEntity child = new AnimaniaAnimalEntity(type, level);
        child.setVariantName(getVariantName());
        child.setGender(AnimalGender.CHILD);
        child.setTamed(isTamed() || (partner instanceof AnimaniaAnimalEntity mate && mate.isTamed()));
        if (isTamed() && getOwnerUUID() != null) child.setOwnerUUID(getOwnerUUID());
        else if (partner instanceof AnimaniaAnimalEntity mate && mate.isTamed()) child.setOwnerUUID(mate.getOwnerUUID());
        return child;
    }

    @Override
    public AnimalGender getGender() {
        int ordinal = entityData.get(GENDER);
        return ordinal >= 0 && ordinal < AnimalGender.values().length ? AnimalGender.values()[ordinal] : AnimalGender.CHILD;
    }

    @Override
    public void setGender(AnimalGender gender) {
        entityData.set(GENDER, (byte) (gender == null ? AnimalGender.CHILD.ordinal() : gender.ordinal()));
    }

    @Override
    public String getVariantName() {
        return entityData.get(VARIANT);
    }

    @Override
    public void setVariantName(String variant) {
        entityData.set(VARIANT, variant == null || variant.isBlank() ? "default" : variant.toLowerCase(Locale.ROOT));
    }

    @Override
    public int getHunger() {
        return entityData.get(HUNGER);
    }

    public void setHunger(int value) {
        entityData.set(HUNGER, Math.max(0, Math.min(100, value)));
    }

    @Override
    public int getThirst() {
        return entityData.get(THIRST);
    }

    public void setThirst(int value) {
        entityData.set(THIRST, Math.max(0, Math.min(100, value)));
    }

    @Override
    public boolean isSleeping() {
        return entityData.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        entityData.set(SLEEPING, sleeping);
    }

    @Override
    public boolean isPlaying() {
        return entityData.get(PLAYING);
    }

    public boolean isSheared() {
        return entityData.get(SHEARED);
    }

    public void setSheared(boolean sheared) {
        entityData.set(SHEARED, sheared);
        if (!sheared) woolRegrowthTicks = 0;
    }

    @Override
    public void setPlaying(boolean playing) {
        entityData.set(PLAYING, playing);
    }

    @Override
    public boolean isPregnant() {
        return entityData.get(PREGNANT);
    }

    @Override
    public int pregnancyTicks() {
        return pregnancyTicks;
    }

    @Override
    public int gestationTicks() {
        return pregnancyDuration();
    }

    public void setPregnant(boolean pregnant) {
        entityData.set(PREGNANT, pregnant && getGender() == AnimalGender.FEMALE && !isSterilized());
    }

    @Override
    public boolean isSterilized() {
        return entityData.get(STERILIZED);
    }

    public void setSterilized(boolean sterilized) {
        entityData.set(STERILIZED, sterilized);
        if (sterilized) setPregnant(false);
    }

    /** Stable taming facade used by the Cats&Dogs addon and probe providers. */
    public boolean isTamed() {
        return entityData.get(TAMED);
    }

    public void setTamed(boolean tamed) {
        entityData.set(TAMED, tamed);
        if (tamed) setPersistenceRequired();
        if (!tamed) {
            setOwnerUUID(null);
            setSitting(false);
        }
    }

    @Nullable
    public java.util.UUID getOwnerUUID() {
        return entityData.get(OWNER).orElse(null);
    }

    public void setOwnerUUID(@Nullable java.util.UUID owner) {
        entityData.set(OWNER, Optional.ofNullable(owner));
    }

    public boolean isSitting() {
        return entityData.get(SITTING);
    }

    public boolean isSaddled() {
        return entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        entityData.set(SADDLED, saddled && isHorseAnimal());
    }

    /** Whether a female milk-producing animal has entered its lactation window. */
    public boolean isMilkReady() {
        return entityData.get(MILK_READY);
    }

    public void setMilkReady(boolean ready) {
        entityData.set(MILK_READY, ready && getGender() == AnimalGender.FEMALE && isAdult() && isMilkSpecies());
    }

    public void setSitting(boolean sitting) {
        entityData.set(SITTING, sitting && isTamed());
        if (sitting) setSleeping(false);
    }

    private boolean ownerMatches(Player player) {
        return getOwnerUUID() != null && getOwnerUUID().equals(player.getUUID());
    }

    private boolean isCompanionAnimal() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && id.getNamespace().equals("animania_catsdogs");
    }

    private boolean isHorseAnimal() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && id.getNamespace().equals("animania_farm")
                && (id.getPath().startsWith("mare_") || id.getPath().startsWith("stallion_") || id.getPath().startsWith("foal_"));
    }

    /**
     * Public addon-neutral hook used by pullable vehicles.  Keeping the
     * family check here means Farm can use the shared vehicle implementation
     * without a Base-to-Farm class dependency, while a child/foal cannot be
     * attached as a draft animal until it has grown into an adult.
     */
    public boolean canPullVehicles() {
        return isHorseAnimal() && isAdult();
    }

    private boolean isCatCompanion() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && id.getNamespace().equals("animania_catsdogs")
                && (id.getPath().startsWith("queen_") || id.getPath().startsWith("tom_") || id.getPath().startsWith("kitten_"));
    }

    private boolean isDogCompanion() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && id.getNamespace().equals("animania_catsdogs")
                && (id.getPath().startsWith("female_") || id.getPath().startsWith("male_") || id.getPath().startsWith("puppy_"));
    }

    private boolean canTameWith(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (isCatCompanion() && entityId != null && AnimaniaApi.matchesRegisteredFood(entityId, stack)) return true;
        return isCatCompanion() ? isCatFood(stack) : stack.is(Items.BONE);
    }

    private boolean isCompanionFood(ItemStack stack) {
        return isCatCompanion() ? isCatFood(stack) : stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON);
    }

    private boolean isCatFood(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH);
    }

    @Override
    public boolean feed(ItemStack stack) {
        if (stack == null || stack.isEmpty() || level().isClientSide || !isAnimaniaFood(stack)) return false;
        interacted = true;
        setHunger(Math.min(100, getHunger() + 20));
        if (isBaby()) {
            ageUp(20);
        } else if (isAdult() && !isSterilized()) {
            setInLove(null);
        }
        return true;
    }

    @Override
    public boolean drink(ItemStack stack) {
        if (stack == null || stack.isEmpty() || level().isClientSide || !isAnimaniaDrink(stack)) return false;
        interacted = true;
        setThirst(100);
        return true;
    }

    @Override
    public boolean play(ItemStack stack) {
        if (stack == null || stack.isEmpty() || level().isClientSide) return false;
        interacted = true;
        setPlaying(true);
        playingTicks = 100;
        setSleeping(false);
        return true;
    }

    @Override
    public boolean canBreedWith(com.animania.api.IAnimaniaAnimal other) {
        if (!(other instanceof AnimaniaAnimalEntity mate) || mate == this) return false;
        ResourceLocation first = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        ResourceLocation second = ForgeRegistries.ENTITY_TYPES.getKey(mate.getType());
        boolean tamedRequirement = AnimaniaApi.requiresTaming(first) || AnimaniaApi.requiresTaming(second);
        if (config(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) && (!interacted || !mate.interacted)) return false;
        if (config(AnimaniaConfig.FEED_TO_BREED, true) && (!isInLove() || !mate.isInLove())) return false;
        if (!breedingCapacityAvailable(mate)) return false;
        return sameSpecies(mate)
                && isAdult() && mate.isAdult()
                && getGender() != mate.getGender()
                && !isSterilized() && !mate.isSterilized()
                && !isPregnant() && !mate.isPregnant()
                && (!tamedRequirement || (isTamed() && mate.isTamed()));
    }

    @Override
    public AnimalSnapshot snapshot() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return new AnimalSnapshot(id, getGender(), isBaby() ? AnimalAge.BABY : AnimalAge.ADULT, getVariantName(),
                getHunger(), getThirst(), isSleeping(), isPregnant(), isSterilized());
    }

    @Override
    public AgeableMob asMob() {
        return this;
    }

    public void ensureValidState() {
        if (getVariantName() == null || getVariantName().isBlank()) setVariantName("default");
        if (getHunger() < 0 || getHunger() > 100) setHunger(100);
        if (getThirst() < 0 || getThirst() > 100) setThirst(100);
        if (!isBaby() && getGender() == AnimalGender.CHILD) setGender(inferGender());
    }

    private AnimalGender inferGender() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return AnimalGender.MALE;
        String path = id.getPath();
        if (path.startsWith("hen_") || path.startsWith("cow_") || path.startsWith("doe_") || path.startsWith("ewe_")
                || path.startsWith("sow_") || path.startsWith("mare_") || path.startsWith("queen_") || path.startsWith("female_")) return AnimalGender.FEMALE;
        if (path.startsWith("chick_") || path.startsWith("calf_") || path.startsWith("kid_") || path.startsWith("lamb_")
                || path.startsWith("piglet_") || path.startsWith("foal_") || path.startsWith("kit_") || path.startsWith("kitten_")
                || path.startsWith("peachick_") || path.startsWith("puppy_")) return AnimalGender.CHILD;
        return AnimalGender.MALE;
    }

    private boolean sameSpecies(AnimaniaAnimalEntity other) {
        ResourceLocation first = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        ResourceLocation second = ForgeRegistries.ENTITY_TYPES.getKey(other.getType());
        return first != null && second != null && first.getNamespace().equals(second.getNamespace())
                && speciesKey(first.getPath()).equals(speciesKey(second.getPath()));
    }

    /**
     * Keep the legacy local population cap server-side.  Counting by species
     * key (rather than by EntityType) means male/female/child registrations
     * share the same cap and prevents a breeding burst from duplicating mobs.
     */
    private boolean breedingCapacityAvailable(AnimaniaAnimalEntity mate) {
        int limit = Math.max(1, config(AnimaniaConfig.ENTITY_BREEDING_LIMIT, 15));
        if (limit <= 0) return true;
        ResourceLocation first = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (first == null) return false;
        int range = Math.max(4, Math.min(32, config(AnimaniaConfig.ANIMAL_CAP_SEARCH_RANGE, 80) / 3));
        int nearby = level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                getBoundingBox().inflate(range), entity -> entity != this && sameSpecies(entity)).size();
        // Include the pair itself in the cap. The proposed child is not yet
        // present, so a limit of N allows at most N existing animals.
        return nearby < limit || mate == null;
    }

    private static String speciesKey(String path) {
        for (String prefix : new String[]{"cow_", "bull_", "calf_", "ewe_", "ram_", "lamb_", "doe_", "buck_", "kid_", "kit_",
                "sow_", "hog_", "piglet_", "hen_", "rooster_", "chick_", "mare_", "stallion_", "foal_",
                "female_", "male_", "puppy_", "queen_", "tom_", "kitten_", "peahen_", "peacock_", "peachick_"}) {
            if (path.startsWith(prefix)) return path.substring(prefix.length());
        }
        return path;
    }

    private static String adultPrefix(String path, AnimalGender gender) {
        boolean female = gender == AnimalGender.FEMALE;
        if (path.startsWith("calf_")) return female ? "cow_" : "bull_";
        if (path.startsWith("kid_") || path.startsWith("kit_")) return female ? "doe_" : "buck_";
        if (path.startsWith("lamb_")) return female ? "ewe_" : "ram_";
        if (path.startsWith("piglet_")) return female ? "sow_" : "hog_";
        if (path.startsWith("chick_")) return female ? "hen_" : "rooster_";
        if (path.startsWith("foal_")) return female ? "mare_" : "stallion_";
        if (path.startsWith("kitten_")) return female ? "queen_" : "tom_";
        if (path.startsWith("puppy_")) return female ? "female_" : "male_";
        if (path.startsWith("peachick_")) return female ? "peahen_" : "peacock_";
        return null;
    }

    private boolean isChildRegistryId() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && adultPrefix(id.getPath(), AnimalGender.FEMALE) != null;
    }

    private static EntityType<?> childType(EntityType<?> parent) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(parent);
        if (id == null) return parent;
        String path = id.getPath();
        String childPrefix = null;
        if (path.startsWith("cow_") || path.startsWith("bull_")) childPrefix = "calf_";
        else if (path.startsWith("ewe_") || path.startsWith("ram_")) childPrefix = "lamb_";
        else if (path.startsWith("doe_") || path.startsWith("buck_")) childPrefix = id.getNamespace().equals("animania_extra") ? "kit_" : "kid_";
        else if (path.startsWith("sow_") || path.startsWith("hog_")) childPrefix = "piglet_";
        else if (path.startsWith("hen_") || path.startsWith("rooster_")) childPrefix = "chick_";
        else if (path.startsWith("mare_") || path.startsWith("stallion_")) childPrefix = "foal_";
        else if (path.startsWith("female_") || path.startsWith("male_")) childPrefix = "puppy_";
        else if (path.startsWith("queen_") || path.startsWith("tom_")) childPrefix = "kitten_";
        else if (path.startsWith("peahen_") || path.startsWith("peacock_")) childPrefix = "peachick_";
        if (childPrefix == null) return parent;
        EntityType<?> child = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id.getNamespace(), childPrefix + speciesKey(path)));
        return child == null ? parent : child;
    }

    private static boolean config(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int config(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double config(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("AnimaniaGender", (byte) getGender().ordinal());
        tag.putString("AnimaniaVariant", getVariantName());
        tag.putInt("AnimaniaHunger", getHunger());
        tag.putInt("AnimaniaThirst", getThirst());
        tag.putBoolean("AnimaniaSleeping", isSleeping());
        tag.putBoolean("AnimaniaPlaying", isPlaying());
        tag.putBoolean("AnimaniaPregnant", isPregnant());
        tag.putBoolean("AnimaniaSterilized", isSterilized());
        tag.putBoolean("AnimaniaSheared", isSheared());
        tag.putBoolean("AnimaniaTamed", isTamed());
        tag.putBoolean("AnimaniaSitting", isSitting());
        tag.putBoolean("AnimaniaSaddled", isSaddled());
        if (getOwnerUUID() != null) tag.putUUID("AnimaniaOwner", getOwnerUUID());
        tag.putInt("AnimaniaPregnancyTicks", pregnancyTicks);
        tag.putInt("AnimaniaWoolRegrowthTicks", woolRegrowthTicks);
        tag.putInt("AnimaniaBoostTicks", boostTicks);
        tag.putInt("AnimaniaStarvationTicks", starvationTicks);
        tag.putInt("AnimaniaEggLayTicks", eggLayTicks);
        tag.putBoolean("AnimaniaMilkReady", isMilkReady());
        tag.putBoolean("AnimaniaInteracted", interacted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int gender = tag.contains("AnimaniaGender") ? tag.getByte("AnimaniaGender") : AnimalGender.CHILD.ordinal();
        setGender(gender >= 0 && gender < AnimalGender.values().length ? AnimalGender.values()[gender] : AnimalGender.CHILD);
        setVariantName(tag.getString("AnimaniaVariant"));
        setHunger(tag.contains("AnimaniaHunger") ? tag.getInt("AnimaniaHunger") : 100);
        setThirst(tag.contains("AnimaniaThirst") ? tag.getInt("AnimaniaThirst") : 100);
        setSleeping(tag.getBoolean("AnimaniaSleeping"));
        setPlaying(tag.getBoolean("AnimaniaPlaying"));
        setPregnant(tag.getBoolean("AnimaniaPregnant"));
        setSterilized(tag.getBoolean("AnimaniaSterilized"));
        setSheared(tag.getBoolean("AnimaniaSheared"));
        setTamed(tag.getBoolean("AnimaniaTamed"));
        if (tag.hasUUID("AnimaniaOwner")) setOwnerUUID(tag.getUUID("AnimaniaOwner"));
        setSitting(tag.getBoolean("AnimaniaSitting"));
        setSaddled(tag.getBoolean("AnimaniaSaddled"));
        pregnancyTicks = tag.getInt("AnimaniaPregnancyTicks");
        woolRegrowthTicks = Math.max(0, tag.getInt("AnimaniaWoolRegrowthTicks"));
        boostTicks = Math.max(0, tag.getInt("AnimaniaBoostTicks"));
        starvationTicks = Math.max(0, tag.getInt("AnimaniaStarvationTicks"));
        eggLayTicks = Math.max(0, tag.getInt("AnimaniaEggLayTicks"));
        setMilkReady(tag.getBoolean("AnimaniaMilkReady"));
        interacted = tag.getBoolean("AnimaniaInteracted");
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        double reduction = Math.max(0.0D, Math.min(1.0D, config(AnimaniaConfig.FALL_DAMAGE_REDUCE_MULTIPLIER, 0.45D)));
        return super.causeFallDamage((float) (fallDistance * reduction), multiplier, source);
    }

    private boolean isMilkable() {
        if (getGender() != AnimalGender.FEMALE || !isAdult() || !isMilkReady()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return false;
        String path = id.getPath();
        return path.startsWith("cow_") || path.startsWith("doe_") || path.startsWith("ewe_") || path.startsWith("mare_");
    }

    @Nullable
    private Item milkBucket() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return null;
        String path = id.getPath();
        String bucketId = null;
        if (path.startsWith("doe_")) bucketId = "milk_goat_bucket";
        else if (path.startsWith("ewe_")) bucketId = "milk_sheep_bucket";
        else if (path.startsWith("cow_")) {
            String family = speciesKey(path);
            if (family.equals("holstein") || family.equals("friesian") || family.equals("jersey")) {
                bucketId = "milk_" + family + "_bucket";
            }
        }
        if (bucketId != null) {
            Item custom = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_farm", bucketId));
            if (custom != null) return custom;
        }
        return Items.MILK_BUCKET;
    }

    private boolean isShearable() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return false;
        String path = id.getPath();
        return path.startsWith("ewe_") || path.startsWith("doe_");
    }

    private void produceFeather() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return;
        String path = id.getPath();
        Item item = null;
        if (path.startsWith("hen_") || path.startsWith("rooster_")) {
            item = Items.FEATHER;
        } else if (path.startsWith("peacock_") || path.startsWith("peahen_")) {
            String color = speciesKey(path);
            item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_extra", color + "_peacock_feather"));
        }
        if (item != null) spawnAtLocation(new ItemStack(item));
    }

    /**
     * Farm addon hook for the legacy hen laying rule. The countdown is kept
     * on the entity and persisted, so a chunk unload cannot duplicate eggs.
     */
    public boolean tryLayFarmEgg(boolean enabled) {
        if (!enabled || level().isClientSide || !isAdult() || getGender() != AnimalGender.FEMALE) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_farm".equals(id.getNamespace()) || !id.getPath().startsWith("hen_")) return false;
        if (eggLayTicks <= 0) eggLayTicks = Math.max(20, config(AnimaniaConfig.LAID_TIMER, 2000)) + random.nextInt(100);
        if (--eggLayTicks > 0) return false;
        eggLayTicks = Math.max(20, config(AnimaniaConfig.LAID_TIMER, 2000)) + random.nextInt(100);
        Item egg = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_farm", "brown_egg"));
        if (egg == null || egg == Items.AIR) return false;
        spawnAtLocation(new ItemStack(egg));
        return true;
    }

    /** Farm addon hook enabling the optional rooster-vs-rooster target goal. */
    public void configureRoosterCombat(boolean enabled) {
        if (!enabled || roosterCombatConfigured || level().isClientSide) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_farm".equals(id.getNamespace()) || !id.getPath().startsWith("rooster_")) return;
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AnimaniaAnimalEntity.class, true,
                target -> target != this && target instanceof AnimaniaAnimalEntity other
                        && ForgeRegistries.ENTITY_TYPES.getKey(other.getType()) != null
                        && ForgeRegistries.ENTITY_TYPES.getKey(other.getType()).getPath().startsWith("rooster_")));
        roosterCombatConfigured = true;
    }

    private boolean isMilkSpecies() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_farm".equals(id.getNamespace())) return false;
        String path = id.getPath();
        return path.startsWith("cow_") || path.startsWith("doe_") || path.startsWith("ewe_") || path.startsWith("mare_");
    }
}
