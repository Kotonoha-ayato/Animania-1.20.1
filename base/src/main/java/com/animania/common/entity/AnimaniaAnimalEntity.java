package com.animania.common.entity;

import com.animania.api.AnimaniaApi;
import com.animania.api.AnimaniaTags;
import com.animania.api.interfaces.IAnimaniaAnimal;
import com.animania.api.interfaces.IBlinking;
import com.animania.api.interfaces.IConvertable;
import com.animania.api.data.AnimalAge;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.AnimalSnapshot;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.advancement.FeedAnimalTrigger;
import com.animania.common.entity.goal.AnimaniaTemptGoal;
import com.animania.common.entity.goal.AnimaniaSleepGoal;
import com.animania.common.entity.goal.AnimaniaPlayGoal;
import com.animania.common.entity.goal.AnimaniaFindMudGoal;
import com.animania.common.entity.goal.AnimaniaPigSnuffleGoal;
import com.animania.common.entity.goal.AnimaniaFindWaterGoal;
import com.animania.common.entity.goal.AnimaniaFindFoodGoal;
import com.animania.common.entity.goal.AnimaniaFindSaltLickGoal;
import com.animania.common.entity.goal.AnimaniaFindNestFoodGoal;
import com.animania.common.entity.goal.AnimaniaRivalHeadbuttGoal;
import com.animania.common.entity.goal.AnimaniaHerdedByGermanShepherdGoal;
import com.animania.common.entity.goal.AnimaniaMateGoal;
import com.animania.common.entity.goal.AnimaniaFollowParentGoal;
import com.animania.common.entity.goal.AnimaniaPanicGoal;
import com.animania.common.entity.goal.AnimaniaWanderAvoidWaterGoal;
import com.animania.common.entity.goal.AnimaniaLookIdleGoal;
import com.animania.common.entity.goal.AnimaniaWatchClosestGoal;
import com.animania.common.entity.goal.AnimaniaSmallCreatureFloatGoal;
import com.animania.common.entity.goal.AnimaniaEatGrassGoal;
import com.animania.common.entity.goal.AnimaniaFollowOwnerGoal;
import com.animania.common.entity.goal.AnimaniaSitGoal;
import com.animania.common.entity.goal.AnimaniaHurtByTargetGoal;
import com.animania.common.entity.goal.AnimaniaNearestAttackableTargetGoal;
import com.animania.common.entity.goal.AnimaniaOwnerHurtByTargetGoal;
import com.animania.common.entity.goal.AnimaniaOwnerHurtTargetGoal;
import com.animania.common.entity.goal.AnimaniaTargetNonTamedGoal;
import com.animania.common.entity.goal.AnimaniaAvoidEntityGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
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
public class AnimaniaAnimalEntity extends Animal implements IAnimaniaAnimal, IBlinking, IConvertable {
    private static final EntityDataAccessor<Byte> GENDER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> THIRST = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PLAYING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MUDDY = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EATING_TICKS = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PREGNANT = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FERTILE = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STERILIZED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<java.util.UUID>> OWNER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<java.util.UUID>> MATE = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<java.util.UUID>> PARENT = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MILK_READY = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IN_BALL = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BALL_COLOR = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WOOL_COLOR = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    /** Synched so a blink is authoritative and remains smooth after a client rejoin. */
    private static final EntityDataAccessor<Integer> BLINK_TIMER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPOOKED = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SPOOKED_TIMER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FIGHTING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<java.util.UUID>> RIVAL = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> FIGHT_TIMER = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GROWTH_PROGRESS = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HAMSTER_FOOD_STACK = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAMSTER_STANDING = SynchedEntityData.defineId(AnimaniaAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String CARRIED_ENTITY_TAG = "AnimaniaCarriedEntity";
    public static final String CARRIED_ANIMAL_TAG = "AnimaniaCarriedAnimal";
    private int pregnancyTicks;
    private int pregnancyDurationTicks;
    private int fertilityCooldownTicks;
    private int lactationTicks;
    private int playingTicks;
    private int woolRegrowthTicks;
    private int boostTicks;
    private int starvationTicks;
    private boolean legacyNamedCombatConfigured;
    private int eggLayTicks;
    private int featherDropTicks;
    private int fedTimer;
    private int wateredTimer;
    private int childGrowthTimer;
    private int crowCooldown;
    private boolean roosterCombatConfigured;
    private AnimaniaPlayGoal playGoal;
    /**
     * 1.12 deliberately kept naturally spawned animals passive until a player
     * interacted with them.  This is server state (not a render hint), so it
     * is persisted with the entity and never inferred from a client packet.
     */
    private boolean interacted;
    private int hamsterStandTicks;
    private int hamsterEatTicks = 5000;

    public AnimaniaAnimalEntity(EntityType<? extends AnimaniaAnimalEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0f);
        // Entity data is defined by the time the constructor returns.  Infer
        // the baseline sex from the legacy registration ID so natural spawns
        // are not all CHILD until their first save/reload.
        AnimalGender inferred = inferGender();
        this.setGender(inferred);
        this.setVariantName(initialVariant());
        // The legacy families intentionally use a staggered first blink.  A
        // single shared timer keeps that behavior while avoiding a separate
        // field/implementation in every addon entity class.
        this.setBlinkTimer(70 + random.nextInt(70));
        this.fedTimer = careTimer(AnimaniaConfig.FEED_TIMER, 12000);
        this.wateredTimer = careTimer(AnimaniaConfig.WATER_TIMER, 12000);
        // Child registry IDs represent the legacy calf/kid/etc. entities. A
        // newly created child must start as a baby even when it came from an
        // egg item or a command (vanilla EntityType instances default to age
        // zero). The server-side growth path below replaces it with the
        // matching adult registry ID when the age reaches zero.
        if (inferred == AnimalGender.CHILD) {
            this.setAge(-childGrowthDuration());
            this.entityData.set(GROWTH_PROGRESS, 0.0F);
        }
        if (AnimaniaFindMudGoal.supports(this)) {
            setPlaying(true);
            playingTicks = Math.max(20, config(AnimaniaConfig.PLAY_TIMER, 12000)) + random.nextInt(100);
        }
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 10.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.22D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 1.0D);
    }

    /** Global Base spawn switch evaluated at every natural-spawn attempt. */
    public static boolean checkAnimalSpawnRules(EntityType<? extends Animal> type, LevelAccessor level,
                                                MobSpawnType reason, BlockPos pos, RandomSource random) {
        boolean enabled;
        try {
            enabled = AnimaniaConfig.ENABLE_NATURAL_SPAWNS.get();
        } catch (IllegalStateException ignored) {
            enabled = true;
        }
        return enabled && Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData groupData, @Nullable CompoundTag spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData, spawnData);
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
            trySpawnNaturalFamilyCompanion(random.nextInt(3));
        }
        return result;
    }

    /** Restores the 1.12 female-spawn family bootstrap (0=male, 1=child). */
    @Nullable
    public AnimaniaAnimalEntity trySpawnNaturalFamilyCompanion(int chooser) {
        if (!(level() instanceof ServerLevel server) || chooser < 0 || chooser > 1) return null;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return null;
        String relatedPath = relatedNaturalFamilyPath(id.getNamespace(), id.getPath(), chooser == 0);
        if (relatedPath == null) return null;
        AABB familyRange = getBoundingBox().inflate(64.0D);
        long nearby = server.getEntitiesOfClass(AnimaniaAnimalEntity.class, familyRange, other -> {
            ResourceLocation otherId = ForgeRegistries.ENTITY_TYPES.getKey(other.getType());
            return otherId != null && otherId.getNamespace().equals(id.getNamespace())
                    && speciesKey(otherId.getPath()).equals(speciesKey(id.getPath()));
        }).size();
        if (nearby > 8) return null;
        EntityType<?> raw = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), relatedPath));
        if (raw == null) return null;
        Entity created = raw.create(server);
        if (!(created instanceof AnimaniaAnimalEntity companion)) return null;
        companion.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        if (chooser == 0) {
            setMateUuid(companion.getUUID());
            companion.setMateUuid(getUUID());
        } else {
            companion.setParentUuid(getUUID());
            companion.setAge(-childGrowthDuration());
        }
        server.addFreshEntity(companion);
        return companion;
    }

    @Nullable
    private static String relatedNaturalFamilyPath(String namespace, String path, boolean male) {
        if (path.startsWith("cow_")) return (male ? "bull_" : "calf_") + path.substring(4);
        if (path.startsWith("sow_")) return (male ? "hog_" : "piglet_") + path.substring(4);
        if (path.startsWith("hen_")) return (male ? "rooster_" : "chick_") + path.substring(4);
        if (path.startsWith("mare_")) return (male ? "stallion_" : "foal_") + path.substring(5);
        if (path.startsWith("ewe_")) return (male ? "ram_" : "lamb_") + path.substring(4);
        if (path.startsWith("doe_")) return (male ? "buck_" : (namespace.equals("animania_extra") ? "kit_" : "kid_")) + path.substring(4);
        if (path.startsWith("queen_")) return (male ? "tom_" : "kitten_") + path.substring(6);
        if (path.startsWith("female_")) return (male ? "male_" : "puppy_") + path.substring(7);
        return null;
    }

    @Override
    protected void registerGoals() {
        if (isHamster()) {
            registerHamsterGoals();
            return;
        }
        if (AnimaniaSmallCreatureFloatGoal.supports(this)) {
            goalSelector.addGoal(AnimaniaSmallCreatureFloatGoal.legacyPriority(this),
                    new AnimaniaSmallCreatureFloatGoal(this));
        }
        if (AnimaniaEatGrassGoal.supports(this)) {
            goalSelector.addGoal(AnimaniaEatGrassGoal.legacyPriority(this), new AnimaniaEatGrassGoal(this));
        }
        registerAvoidEntityGoals();
        if (AnimaniaSitGoal.supports(this)) goalSelector.addGoal(1, new AnimaniaSitGoal(this));
        if (AnimaniaFollowOwnerGoal.supports(this)) goalSelector.addGoal(7, new AnimaniaFollowOwnerGoal(this));
        if (AnimaniaPanicGoal.supports(this)) {
            goalSelector.addGoal(0, new AnimaniaPanicGoal(this, AnimaniaPanicGoal.legacySpeed(this)));
        }
        goalSelector.addGoal(1, new AnimaniaMateGoal(this, 1.0D));
        goalSelector.addGoal(2, new AnimaniaTemptGoal(this, AnimaniaTemptGoal.legacySpeed(this),
                AnimaniaTemptGoal.legacyScaredByMovement(this)));
        goalSelector.addGoal(3, new AnimaniaFollowParentGoal(this, 1.1D));
        goalSelector.addGoal(3, new AnimaniaFindWaterGoal(this));
        if (AnimaniaFindNestFoodGoal.supports(this)) {
            goalSelector.addGoal(3, new AnimaniaFindNestFoodGoal(this));
        }
        if (AnimaniaRivalHeadbuttGoal.supports(this)) {
            goalSelector.addGoal(4, new AnimaniaRivalHeadbuttGoal(this));
        }
        if (AnimaniaHerdedByGermanShepherdGoal.supports(this)) {
            goalSelector.addGoal(1, new AnimaniaHerdedByGermanShepherdGoal(this));
        }
        goalSelector.addGoal(3, new AnimaniaFindFoodGoal(this));
        goalSelector.addGoal(4, new AnimaniaFindSaltLickGoal(this));
        if (AnimaniaPlayGoal.supports(this)) goalSelector.addGoal(4, playGoal = new AnimaniaPlayGoal(this));
        if (AnimaniaFindMudGoal.supports(this)) goalSelector.addGoal(1, new AnimaniaFindMudGoal(this));
        if (AnimaniaFindMudGoal.supports(this)) goalSelector.addGoal(11, new AnimaniaPigSnuffleGoal(this));
        goalSelector.addGoal(5, new AnimaniaSleepGoal(this));
        if (AnimaniaWanderAvoidWaterGoal.supports(this)) {
            goalSelector.addGoal(6, new AnimaniaWanderAvoidWaterGoal(this,
                    AnimaniaWanderAvoidWaterGoal.legacySpeed(this)));
        }
        if (AnimaniaWatchClosestGoal.supports(this)) goalSelector.addGoal(7, new AnimaniaWatchClosestGoal(this));
        if (AnimaniaLookIdleGoal.supports(this)) goalSelector.addGoal(8, new AnimaniaLookIdleGoal(this));
        // Cats and dogs retain the legacy companion combat intent while
        // remaining server-authoritative and opt-out through the shared rule.
        if (attacksAllowed() && isCompanionAnimal()) {
            goalSelector.addGoal(4, new MeleeAttackGoal(this, isDogCompanion() ? 1.15D : 1.0D, true));
            if (isCatCompanion()) goalSelector.addGoal(5, new LeapAtTargetGoal(this, 0.4F));
            targetSelector.addGoal(1, new AnimaniaHurtByTargetGoal(this));
            if (isDogCompanion()) {
                targetSelector.addGoal(2, new AnimaniaOwnerHurtByTargetGoal(this));
                targetSelector.addGoal(3, new AnimaniaOwnerHurtTargetGoal(this));
            }
            targetSelector.addGoal(4, new AnimaniaNearestAttackableTargetGoal<>(this, AbstractSkeleton.class, true,
                    target -> !isTamed()));
            if (isDogCompanion()) {
                targetSelector.addGoal(5, new AnimaniaTargetNonTamedGoal<>(this, Sheep.class, true,
                        target -> true));
                targetSelector.addGoal(6, new AnimaniaTargetNonTamedGoal<>(this, Rabbit.class, true,
                        target -> true));
            } else {
                targetSelector.addGoal(5, new AnimaniaTargetNonTamedGoal<>(this, Chicken.class, true,
                        target -> true));
            }
        }
    }

    /** Exact task ordering from EntityHamster#initAI in the 1.12 baseline. */
    private void registerHamsterGoals() {
        goalSelector.addGoal(1, new AnimaniaSitGoal(this));
        goalSelector.addGoal(1, new AnimaniaPanicGoal(this, 1.4D));
        goalSelector.addGoal(2, new AnimaniaSmallCreatureFloatGoal(this));
        goalSelector.addGoal(3, new AnimaniaFindWaterGoal(this));
        goalSelector.addGoal(3, new AnimaniaFindFoodGoal(this));
        goalSelector.addGoal(4, new FleeSunGoal(this, 1.0D));
        goalSelector.addGoal(5, new AnimaniaWanderAvoidWaterGoal(this, 1.1D));
        goalSelector.addGoal(6, new AnimaniaTemptGoal(this, 1.2D, false));
        goalSelector.addGoal(7, new AnimaniaFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        goalSelector.addGoal(8, new AnimaniaWatchClosestGoal(this));
        goalSelector.addGoal(9, new AnimaniaLookIdleGoal(this));
        goalSelector.addGoal(10, new AnimaniaSleepGoal(this));
        targetSelector.addGoal(0, new AnimaniaHurtByTargetGoal(this));
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        configureLegacyNamedCombat();
    }

    private void configureLegacyNamedCombat() {
        if (legacyNamedCombatConfigured || !attacksAllowed()) return;
        boolean pepe = isNamedFrog("Pepe");
        boolean killer = isNamedExtraRabbit("Killer");
        if (!pepe && !killer) return;
        legacyNamedCombatConfigured = true;
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, pepe ? 0.5F : 0.7F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 2.0D, true));
        targetSelector.addGoal(1, new AnimaniaHurtByTargetGoal(this));
        if (killer) {
            targetSelector.addGoal(2, new AnimaniaNearestAttackableTargetGoal<>(this, Player.class,
                    0, true, false, target -> true));
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(50.0D);
            setHealth(50.0F);
        } else {
            targetSelector.addGoal(2, new AnimaniaNearestAttackableTargetGoal<>(this, AnimaniaAnimalEntity.class,
                    0, true, false, target -> target instanceof AnimaniaAnimalEntity animal
                    && (animal.registryPath().startsWith("ferret_") || animal.registryPath().startsWith("hedgehog"))));
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20.0D);
            setHealth(20.0F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        DamageSource special = null;
        float amount = 0.0F;
        if (isNamedFrog("Pepe")) {
            special = com.animania.common.AnimaniaDamageSources.pepe(level());
            amount = 2.0F;
        } else if (isNamedExtraRabbit("Killer")) {
            special = com.animania.common.AnimaniaDamageSources.killerRabbit(level());
            amount = 5.0F;
        }
        if (special == null) return super.doHurtTarget(target);
        boolean first = target.hurt(special, amount);
        target.hurt(special, amount);
        if (first && target instanceof net.minecraft.world.entity.LivingEntity living) {
            living.knockback(1.0D, getX() - target.getX(), getZ() - target.getZ());
        }
        return first;
    }

    private boolean isNamedFrog(String name) {
        return registryPath().equals("frog") && hasCustomName() && name.equals(getCustomName().getString());
    }

    private boolean isNamedExtraRabbit(String name) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        String path = registryPath();
        return id != null && "animania_extra".equals(id.getNamespace())
                && (path.startsWith("buck_") || path.startsWith("doe_") || path.startsWith("kit_"))
                && hasCustomName() && name.equals(getCustomName().getString());
    }

    private void registerAvoidEntityGoals() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return;
        if ("animania_catsdogs".equals(id.getNamespace()) && isCatCompanion()) {
            goalSelector.addGoal(9, new AnimaniaAvoidEntityGoal<>(this, Player.class, 16.0F, 0.8D, 1.33D));
        } else if ("animania_farm".equals(id.getNamespace()) && (id.getPath().startsWith("ewe_")
                || id.getPath().startsWith("ram_") || id.getPath().startsWith("lamb_"))) {
            goalSelector.addGoal(9, new AnimaniaAvoidEntityGoal<>(this, Wolf.class, 24.0F, 2.0D, 2.2D));
        } else if ("animania_farm".equals(id.getNamespace()) && (id.getPath().startsWith("doe_")
                || id.getPath().startsWith("buck_") || id.getPath().startsWith("kid_"))) {
            goalSelector.addGoal(9, new AnimaniaAvoidEntityGoal<>(this, Wolf.class, 20.0F, 2.2D, 2.2D));
        } else if ("animania_extra".equals(id.getNamespace()) && (id.getPath().startsWith("doe_")
                || id.getPath().startsWith("buck_") || id.getPath().startsWith("kit_"))) {
            goalSelector.addGoal(9, new AnimaniaAvoidEntityGoal<>(this, Wolf.class, 24.0F, 3.0D, 3.5D));
            goalSelector.addGoal(9, new AnimaniaAvoidEntityGoal<>(this, Monster.class, 16.0F, 2.2D, 2.2D));
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
        entityData.define(MUDDY, false);
        entityData.define(EATING_TICKS, 0);
        entityData.define(PREGNANT, false);
        entityData.define(FERTILE, true);
        entityData.define(STERILIZED, false);
        entityData.define(SHEARED, false);
        entityData.define(TAMED, false);
        entityData.define(SITTING, false);
        entityData.define(OWNER, Optional.empty());
        entityData.define(MATE, Optional.empty());
        entityData.define(PARENT, Optional.empty());
        entityData.define(SADDLED, false);
        entityData.define(MILK_READY, false);
        entityData.define(IN_BALL, false);
        entityData.define(BALL_COLOR, 0);
        entityData.define(WOOL_COLOR, DyeColor.WHITE.getId());
        entityData.define(BLINK_TIMER, 100);
        entityData.define(SPOOKED, false);
        entityData.define(SPOOKED_TIMER, 0);
        entityData.define(FIGHTING, false);
        entityData.define(RIVAL, Optional.empty());
        entityData.define(FIGHT_TIMER, 0);
        entityData.define(GROWTH_PROGRESS, 1.0F);
        entityData.define(HAMSTER_FOOD_STACK, 0);
        entityData.define(HAMSTER_STANDING, false);
    }

    @Override
    public void tick() {
        // Vanilla AgeableMob advances age unconditionally. Legacy Animania
        // children only grow after each configured interval while cared for,
        // so preserve their pre-vanilla age and advance it explicitly below.
        int legacyChildAge = !level().isClientSide && isChildRegistryId() ? getAge() : 0;
        super.tick();
        tickBlinkTimer();
        if (level().isClientSide) return;
        if (legacyChildAge < 0 && isChildRegistryId()) setAge(legacyChildAge);
        entityData.set(GROWTH_PROGRESS, calculateGrowthProgress());
        if (isSitting() || isSleeping()) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }
        tickHamsterState();
        if (config(AnimaniaConfig.AMBIANCE_MODE, false)) {
            // Ambiance mode keeps the care meters full and disables all
            // starvation pressure while retaining the visible state fields.
            setHunger(100);
            setThirst(100);
            fedTimer = careTimer(AnimaniaConfig.FEED_TIMER, 12000);
            wateredTimer = careTimer(AnimaniaConfig.WATER_TIMER, 12000);
            starvationTicks = 0;
        } else {
            boolean careTimersActive = !config(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) || interacted;
            if (careTimersActive) {
                if (fedTimer > 0 && --fedTimer == 0) setHunger(0);
                if (wateredTimer > 0 && --wateredTimer == 0) setThirst(0);
                if (tickCount % Math.max(20, config(AnimaniaConfig.HUNGER_INTERVAL, 2400)) == 0) setHunger(getHunger() - 1);
                if (tickCount % Math.max(20, config(AnimaniaConfig.THIRST_INTERVAL, 1800)) == 0) setThirst(getThirst() - 1);
            }
            if (getHunger() <= 0 && getThirst() <= 0) {
                addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2, 1, false, false));
            } else if (getHunger() <= 0 || getThirst() <= 0) {
                addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2, 0, false, false));
            }
            if (config(AnimaniaConfig.ANIMALS_STARVE, false) && (getHunger() <= 0 || getThirst() <= 0)) {
                if (!isSleeping() && ++starvationTicks >= Math.max(20, config(AnimaniaConfig.STARVATION_TIMER, 400))) {
                    starvationTicks = 0;
                    hurt(level().damageSources().starve(), 4.0F);
                }
            } else {
                starvationTicks = 0;
            }
        }
        if (playingTicks > 0 && --playingTicks == 0) setPlaying(false);
        if (getSpookedTimer() > 0) {
            setSpookedTimer(getSpookedTimer() - 1);
            if (getSpookedTimer() == 0) setSpooked(false);
        }
        if (getFightTimer() > 0) setFightTimer(getFightTimer() - 1);
        if (tickCount % 60 == 0 && shouldShowUnhappyParticles() && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, getRandomX(1.0D), getRandomY() + 0.5D,
                    getRandomZ(1.0D), 1, 0.001D, 0.001D, 0.001D, 0.0D);
        }
        if (getEatingTicks() > 0) setEatingTicks(getEatingTicks() - 1);
        if (AnimaniaFindMudGoal.supports(this)) {
            BlockPos feet = blockPosition();
            boolean inMud = isMudBlock(level().getBlockState(feet)) || isMudBlock(level().getBlockState(feet.below()));
            if (inMud) enterMud();
            else if (isInWaterRainOrBubble()) setMuddy(false);
        }
        if (boostTicks > 0) boostTicks--;
        if (isSheared() && --woolRegrowthTicks <= 0) setSheared(false);
        if (isAdult() && config(AnimaniaConfig.BIRDS_DROP_FEATHERS, true) && canDropFeather()) {
            if (featherDropTicks <= 0) featherDropTicks = nextFeatherDropTicks();
            if (--featherDropTicks <= 0) {
                produceFeather();
                featherDropTicks = nextFeatherDropTicks();
            }
        }
        if (!isFertile() && fertilityCooldownTicks > 0 && --fertilityCooldownTicks == 0 && !isSterilized()) {
            setFertile(true);
        }
        if (isMilkReady() && lactationTicks > 0 && --lactationTicks == 0) setMilkReady(false);
        if (isBaby() && isChildRegistryId() && getAge() < 0) {
            int interval = childGrowthInterval();
            if (++childGrowthTimer >= interval && getHunger() > 0 && getThirst() > 0 && !isSleeping()) {
                childGrowthTimer = 0;
                setAge(Math.min(0, getAge() + interval));
            }
        }
        // AgeableMob also advances its age one tick at a time. If that
        // vanilla path reaches zero before the 20-tick Animania growth step,
        // still perform the registry-ID transition on the first adult tick.
        if (getAge() >= 0 && isChildRegistryId()) {
            growIntoAdultVariant();
        }
        if (isPregnant() && ++pregnancyTicks >= pregnancyDuration()) giveBirth();
        tickRoosterCrow();
    }

    /** Restores the 1.12 cheek-pouch, alert-standing and self-heal cycle. */
    private void tickHamsterState() {
        if (!isHamster()) return;
        if (isHamsterStanding()) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (--hamsterStandTicks <= 0 && random.nextInt(10) == 0) setHamsterStanding(false, 0);
        } else if (!isSitting() && !isSleeping() && !isInBall()
                && random.nextInt(20) == 0 && random.nextInt(20) == 0) {
            setHamsterStanding(true, 30);
        }
        if (getHamsterFoodStack() > 0) {
            if (getHealth() < getMaxHealth()) {
                setHamsterFoodStack(getHamsterFoodStack() - 1);
                heal(1.0F);
                hamsterEatTicks = 5000;
            } else if (hamsterEatTicks-- <= 0) {
                if (random.nextInt(30) == 0 && random.nextInt(30) == 0) {
                    setHamsterFoodStack(getHamsterFoodStack() - 1);
                    heal(1.0F);
                    hamsterEatTicks = 5000;
                }
            }
        }
    }

    /** Client-synchronized 0..1 fraction of the legacy 0.00..0.85 child growth cycle. */
    public float growthProgress() {
        return entityData.get(GROWTH_PROGRESS);
    }

    private float calculateGrowthProgress() {
        if (!isChildRegistryId() || getAge() >= 0) return 1.0F;
        return net.minecraft.util.Mth.clamp(1.0F + (float) getAge() / childGrowthDuration(), 0.0F, 1.0F);
    }

    private void tickBlinkTimer() {
        if (level().isClientSide) return;
        int timer = getBlinkTimer();
        if (timer < 0) return;
        if (timer == 0) setBlinkTimer(100 + random.nextInt(100));
        else setBlinkTimer(timer - 1);
    }

    /** Modern collision hook for the three fainting-goat registrations. */
    @Override
    public void push(Entity entity) {
        if (!level().isClientSide && isFaintingGoat() && entity instanceof Player player && player.isSprinting()) {
            setSpooked(true);
            setSpookedTimer(20);
        }
        super.push(entity);
    }

    public boolean isFaintingGoat() {
        return "animania_farm".equals(registryNamespace()) && registryPath().contains("fainting");
    }

    public boolean isSpooked() { return entityData.get(SPOOKED); }
    public void setSpooked(boolean value) { entityData.set(SPOOKED, value); }
    public int getSpookedTimer() { return entityData.get(SPOOKED_TIMER); }
    public void setSpookedTimer(int ticks) { entityData.set(SPOOKED_TIMER, Math.max(0, ticks)); }
    public boolean isFighting() { return entityData.get(FIGHTING); }
    public void setFighting(boolean value) { entityData.set(FIGHTING, value); }
    public java.util.UUID getRivalUuid() { return entityData.get(RIVAL).orElse(null); }
    public void setRivalUuid(java.util.UUID value) { entityData.set(RIVAL, Optional.ofNullable(value)); }
    public int getFightTimer() { return entityData.get(FIGHT_TIMER); }
    public void setFightTimer(int ticks) { entityData.set(FIGHT_TIMER, Math.max(0, ticks)); }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Passengers cannot be damaged by their rider's attack in the legacy
        // rules. A sleeping animal wakes on every real hit, but starvation is
        // suppressed while it is asleep so the server cannot kill it between
        // wake-up packets.
        if (isPassenger()) return false;
        if (isSleeping() && source.is(net.minecraft.world.damagesource.DamageTypes.STARVE)) return false;
        if (isSleeping()) setSleeping(false);
        if (isSitting()) setSitting(false);
        boolean hurt = super.hurt(source, amount);
        return hurt;
    }

    private void tickRoosterCrow() {
        if (!registryPath().startsWith("rooster_")) return;
        if (crowCooldown > 0) crowCooldown--;
        long time = level().getDayTime() % 24000L;
        if (crowCooldown > 0 || (time >= 500L && time <= 23250L)) return;
        SoundEvent crow = legacySound("crow1", "crow2", "crow3");
        if (crow != null) {
            float modular = (random.nextFloat() * random.nextInt(3)) / 10.0F
                    * (random.nextBoolean() ? 1.0F : -1.0F);
            level().playSound(null, blockPosition(), crow, getSoundSource(), 0.65F, 1.0F + modular);
        }
        crowCooldown = 200 + random.nextInt(200);
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        if (isSleeping()) return null;
        String path = registryPath();
        if (path.startsWith("rooster_") || path.startsWith("hen_") || path.startsWith("chick_"))
            return legacySound("cluck1", "cluck2", "cluck3", "cluck4", "cluck5", "cluck6");
        if (path.startsWith("bull_"))
            return legacySound("bullmoo1", "bullmoo2", "bullmoo3", "bullmoo4", "bullmoo5", "bullmoo6", "bullmoo7", "bullmoo8", "moo4", "moo8");
        if (path.startsWith("cow_")) return legacySound("moo1", "moo3", "moo4", "moo5", "moo6", "moo7", "moo8");
        if (path.startsWith("calf_")) return legacySound("moocalf1", "moocalf2", "moocalf3");
        if (path.startsWith("stallion_") || path.startsWith("mare_") || path.startsWith("foal_"))
            return legacySound("horseliving1", "horseliving2", "horseliving3", "horseliving4", "horseliving5", "horseliving6");
        if (path.startsWith("hog_")) return legacySound("hog1", "hog2", "hog3", "hog4", "hog5", "pig1", "pig2", "pig4");
        if (path.startsWith("sow_")) return legacySound("pig1", "pig2", "pig4", "pig5", "pig6", "pig7");
        if (path.startsWith("piglet_")) return legacySound("piglet1", "piglet2", "piglet3", "pig1", "pig2");
        if (path.startsWith("ram_") || path.startsWith("ewe_"))
            return legacySound("sheepliving1", "sheepliving2", "sheepliving3", "sheepliving4", "sheepliving5", "sheepliving6", "sheepliving7");
        if (path.startsWith("lamb_")) return legacySound("lambliving1", "lambliving2");
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (type != null && "animania_farm".equals(type.getNamespace())
                && (path.startsWith("buck_") || path.startsWith("doe_")))
            return legacySound("goatliving1", "goatliving2", "goatliving3", "goatliving4", "goatliving5");
        if (path.startsWith("kid_")) return legacySound("kidliving1", "kidliving2", "kidliving3");
        if (path.startsWith("ferret_")) return legacySound("ferretliving1", "ferretliving2", "ferretliving3", "ferretliving4", "ferretliving5", "ferretliving6");
        if (path.equals("hamster")) return legacySound("hamsterliving1", "hamsterliving2", "hamsterliving3");
        if (path.startsWith("hedgehog"))
            return legacySound("hedgehogliving1", "hedgehogliving2", "hedgehogliving3", "hedgehogliving4", "hedgehogliving5");
        if (type != null && "animania_extra".equals(type.getNamespace())
                && (path.startsWith("buck_") || path.startsWith("doe_") || path.startsWith("kit_")))
            return legacySound("rabbit1", "rabbit2", "rabbit3", "rabbit4");
        if (path.startsWith("peacock_") || path.startsWith("peahen_") || path.startsWith("peachick_"))
            return legacySound("peacock1", "peacock2", "peacock3", "peacock4", "peacock5", "peacock7", "peacock8", "peacock9", "peacock10");
        if (path.equals("frog")) return legacySound("frogliving1", "frogliving2", "frogliving3");
        if (path.equals("dartfrog")) return legacySound("dartfrogliving1", "dartfrogliving2", "dartfrogliving3", "dartfrogliving4");
        if (path.equals("toad")) return legacySound("toadliving1", "toadliving2", "toadliving3", "toadliving4");
        return super.getAmbientSound();
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        String path = registryPath();
        if (path.startsWith("rooster_")) return legacySound("hurt1", "hurt2");
        if (path.startsWith("bull_")) return legacySound("angrybull1", "angrybull2", "angrybull3");
        if (path.startsWith("cow_")) return legacySound("cowhurt1", "cowhurt2");
        if (path.startsWith("calf_")) return legacySound("moocalf1", "moocalf2", "moocalf3");
        if (path.startsWith("stallion_") || path.startsWith("mare_") || path.startsWith("foal_"))
            return legacySound("horsehurt1", "horsehurt2", "horsehurt3");
        if (path.startsWith("hog_") || path.startsWith("sow_")) return legacySound("pighurt1", "pighurt2", "pig3");
        if (path.startsWith("piglet_")) return legacySound("piglethurt1", "piglethurt2", "piglethurt3");
        if (path.startsWith("ram_") || path.startsWith("ewe_")) return legacySound("sheephurt1", "sheepliving7");
        if (path.startsWith("lamb_")) return legacySound("sheephurt1", "lambliving2");
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (type != null && "animania_farm".equals(type.getNamespace())
                && (path.startsWith("buck_") || path.startsWith("doe_")))
            return legacySound("goathurt1", "goathurt2", "goatliving3");
        if (path.startsWith("kid_")) return legacySound("kidhurt1", "kidhurt2");
        if (path.startsWith("ferret_")) return legacySound("ferrethurt1");
        if (path.equals("hamster")) return legacySound("hamsterhurt1");
        if (path.startsWith("hedgehog")) return legacySound("hedgehoghurt1", "hedgehoghurt2");
        if (type != null && "animania_extra".equals(type.getNamespace())
                && (path.startsWith("buck_") || path.startsWith("doe_") || path.startsWith("kit_")))
            return legacySound("rabbithurt1", "rabbithurt2");
        if (path.startsWith("peacock_") || path.startsWith("peahen_") || path.startsWith("peachick_"))
            return legacySound("peacockhurt1", "peacockhurt2");
        return super.getHurtSound(source);
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        String path = registryPath();
        if (path.startsWith("rooster_")) return legacySound("death1", "death2");
        if (path.startsWith("bull_") || path.startsWith("cow_")) return legacySound("cowdeath1", "cowdeath2");
        if (path.equals("hamster")) return legacySound("hamsterhurt1");
        SoundEvent hurt = getHurtSound(level().damageSources().generic());
        return hurt != null ? hurt : super.getDeathSound();
    }

    @Nullable
    private SoundEvent legacySound(String... ids) {
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (type == null || ids.length == 0) return null;
        String namespace = type.getNamespace();
        if (!namespace.equals("animania_farm") && !namespace.equals("animania_extra")) return null;
        for (int attempt = 0; attempt < ids.length; attempt++) {
            String selected = ids[random.nextInt(ids.length)].toLowerCase(Locale.ROOT);
            SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(
                    ResourceLocation.fromNamespaceAndPath(namespace, selected));
            if (event != null) return event;
        }
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // The legacy hamster drops the ball that enclosed it.  Resolve the
        // addon item by registry id so Base never links against Extra.
        if (isHamster() && isInBall() && !level().isClientSide) {
            int color = getBallColor();
            Item ball = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_extra",
                    color == 16 ? "hamster_ball_clear" : "hamster_ball_colored"));
            if (ball != null && ball != Items.AIR) {
                ItemStack returned = new ItemStack(ball);
                if (color != 16) returned.getOrCreateTag().putInt("BallColor", color);
                spawnAtLocation(returned);
            }
            setInBall(false);
        }
    }

    private int pregnancyDuration() {
        if (pregnancyDurationTicks > 0) return pregnancyDurationTicks;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        int configured = Math.max(200, config(AnimaniaConfig.GESTATION_TICKS, 20000));
        if (id != null && (id.getNamespace().equals("animania") || id.getNamespace().startsWith("animania_"))) {
            return configured;
        }
        return AnimaniaApi.species(id).map(SpeciesDefinition::gestationTicks).orElse(configured);
    }

    private int newPregnancyDuration() {
        return pregnancyDuration() + random.nextInt(isHorseAnimal() ? 400 : 200);
    }

    private void giveBirth() {
        if (getGender() != AnimalGender.FEMALE) {
            setPregnant(false);
            pregnancyTicks = 0;
            return;
        }
        setPregnant(false);
        pregnancyTicks = 0;
        pregnancyDurationTicks = 0;
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
            child.setAge(-childGrowthDuration());
            child.setGender(AnimalGender.CHILD);
            child.setParentUuid(getUUID());
            if (hasInteracted()) child.markInteracted();
            child.moveTo(getX() + (index * 0.25D), getY(), getZ() + (index * 0.25D), getYRot(), 0.0f);
            ((ServerLevel) level()).addFreshEntity(child);
        }
        if (isMilkSpecies()) {
            setMilkReady(true);
            lactationTicks = childGrowthDuration();
        }
        setFertile(false);
        fertilityCooldownTicks = Math.max(1, config(AnimaniaConfig.GESTATION_TICKS, 20000) / 9
                + random.nextInt(50));
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
        Entity created = adultType.create(server);
        if (!(created instanceof AnimaniaAnimalEntity adult)) return;
        adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        adult.setDeltaMovement(getDeltaMovement());
        adult.setAge(0);
        adult.setGender(adultGender);
        adult.setVariantName(getVariantName());
        adult.setHunger(getHunger());
        adult.setThirst(getThirst());
        adult.setSterilized(isSterilized());
        adult.setTamed(isTamed());
        adult.setOwnerUUID(getOwnerUUID());
        adult.setSitting(isSitting());
        adult.interacted = interacted;
        adult.setCustomName(getCustomName());
        adult.setCustomNameVisible(isCustomNameVisible());
        adult.setSilent(isSilent());
        adult.setNoGravity(isNoGravity());
        adult.setInvulnerable(isInvulnerable());
        adult.setHealth(Math.min(getHealth(), adult.getMaxHealth()));
        adult.setPersistenceRequired();
        if (!server.addFreshEntity(adult)) return;
        if (isPassenger()) stopRiding();
        discard();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isHamster()) {
            InteractionResult ballResult = interactHamsterBall(player, hand, stack);
            if (ballResult != null) return ballResult;
            if (stack.isEmpty() && isTamed() && !player.isCrouching() && !isSleeping()) {
                if (!level().isClientSide) {
                    setSitting(!isSitting());
                    setHamsterStanding(false, 0);
                    getNavigation().stop();
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
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
        if (isAnimaniaDrink(stack)) {
            if (isSleeping()) return InteractionResult.PASS;
            if (!level().isClientSide && !drink(stack)) return InteractionResult.PASS;
            if (!level().isClientSide && !player.getAbilities().instabuild
                    && config(AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING, true)) consumeDrinkContainer(player, hand, stack);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(AnimaniaTags.ANIMAL_FEED) || isAnimaniaFood(stack)) {
            if (isSleeping()) return InteractionResult.PASS;
            if (!level().isClientSide) {
                if (isHamster() && player.isShiftKeyDown()
                        && com.animania.common.AnimaniaSupporters.contains(player.getUUID())) {
                    setVariantName("gold");
                }
                if (isHamster()) {
                    if (!isTamed()) {
                        setTamed(true);
                        setOwnerUUID(player.getUUID());
                        setSitting(false);
                    }
                    if (getHamsterFoodStack() < 5) setHamsterFoodStack(getHamsterFoodStack() + 1);
                    else heal(1.0F);
                    setHamsterStanding(true, 100);
                }
                ItemStack fedItem = stack.copyWithCount(1);
                if (!feed(stack)) return InteractionResult.PASS;
                if (player instanceof ServerPlayer serverPlayer) {
                    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(getType());
                    if (entityId != null) FeedAnimalTrigger.INSTANCE.trigger(serverPlayer, fedItem, entityId);
                }
            }
            if (!level().isClientSide && !player.getAbilities().instabuild
                    && config(AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING, true)) stack.shrink(1);
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
        if (stack.getItem() instanceof DyeItem dye && isLegacySheep() && getWoolColor() != dye.getDyeColor().getId()) {
            if (!level().isClientSide) {
                setWoolColor(dye.getDyeColor().getId());
                interacted = true;
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.DYE_USE, getSoundSource(), 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.SHEARS) && !isBaby() && isShearable() && !isSheared()) {
            if (!level().isClientSide) {
                interacted = true;
                setSheared(true);
                woolRegrowthTicks = nextWoolRegrowthTicks();
                spawnAtLocation(new ItemStack(woolDropItem(), 1 + random.nextInt(3)));
                stack.hurtAndBreak(1, player, broken -> player.broadcastBreakEvent(hand));
                level().playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, getSoundSource(), 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** True for the Extra addon's single legacy hamster registration. */
    public boolean isHamster() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && "animania_extra".equals(id.getNamespace()) && "hamster".equals(id.getPath());
    }

    public int getHamsterFoodStack() {
        return isHamster() ? Math.max(0, Math.min(5, entityData.get(HAMSTER_FOOD_STACK))) : 0;
    }

    public void setHamsterFoodStack(int count) {
        if (isHamster()) entityData.set(HAMSTER_FOOD_STACK, Math.max(0, Math.min(5, count)));
    }

    public boolean isHamsterStanding() {
        return isHamster() && entityData.get(HAMSTER_STANDING);
    }

    public void setHamsterStanding(boolean standing, int ticks) {
        if (!isHamster()) return;
        entityData.set(HAMSTER_STANDING, standing);
        hamsterStandTicks = standing ? Math.max(1, ticks) : 0;
        if (standing) {
            setSitting(false);
            setSleeping(false);
        }
    }

    public boolean isInBall() {
        return entityData.get(IN_BALL);
    }

    public void setInBall(boolean inBall) {
        entityData.set(IN_BALL, inBall);
        if (inBall) getNavigation().stop();
        if (level() != null && !level().isClientSide) {
            level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC,
                    getSoundSource(), 0.3F, inBall ? 1.6F : 1.3F);
        }
    }

    public int getBallColor() {
        return Math.max(0, Math.min(16, entityData.get(BALL_COLOR)));
    }

    public void setBallColor(int color) {
        entityData.set(BALL_COLOR, Math.max(0, Math.min(16, color)));
    }

    public int getWoolColor() {
        return Math.max(0, Math.min(15, entityData.get(WOOL_COLOR)));
    }

    public void setWoolColor(int color) {
        entityData.set(WOOL_COLOR, Math.max(0, Math.min(15, color)));
    }

    public boolean isLegacySheep() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_farm".equals(id.getNamespace())) return false;
        String path = id.getPath();
        return path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_");
    }

    private InteractionResult interactHamsterBall(Player player, InteractionHand hand, ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean clearBall = itemId != null && "animania_extra".equals(itemId.getNamespace())
                && "hamster_ball_clear".equals(itemId.getPath());
        boolean coloredBall = itemId != null && "animania_extra".equals(itemId.getNamespace())
                && "hamster_ball_colored".equals(itemId.getPath());
        if ((clearBall || coloredBall) && !isInBall() && !isSleeping()) {
            if (!level().isClientSide) {
                setSitting(false);
                setBallColor(clearBall ? 16 : ballColorFromStack(stack));
                setInBall(true);
                markInteracted();
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.isEmpty() && isInBall() && !isSleeping()) {
            if (!level().isClientSide) {
                int color = getBallColor();
                setInBall(false);
                if (!player.getAbilities().instabuild) {
                    Item ball = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_extra",
                            color == 16 ? "hamster_ball_clear" : "hamster_ball_colored"));
                    if (ball != null && ball != Items.AIR) {
                        ItemStack returned = new ItemStack(ball);
                        if (color != 16) returned.getOrCreateTag().putInt("BallColor", color);
                        if (!player.addItem(returned)) player.drop(returned, false);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.isEmpty() && player.isCrouching() && isTamed()
                && !isSleeping() && !isInBall() && !hasCarriedAnimal(player)) {
            if (!level().isClientSide) {
                CompoundTag animal = new CompoundTag();
                addAdditionalSaveData(animal);
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
                if (id != null) {
                    player.getPersistentData().putString(CARRIED_ENTITY_TAG, id.toString());
                    player.getPersistentData().put(CARRIED_ANIMAL_TAG, animal);
                    if (player instanceof ServerPlayer serverPlayer) {
                        com.animania.network.AnimaniaNetwork.syncCarried(serverPlayer);
                    }
                    discard();
                    player.swing(hand);
                    level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP,
                            getSoundSource(), 0.8F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return null;
    }

    private static int ballColorFromStack(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("BallColor")) return 0;
        return Math.max(0, Math.min(15, stack.getTag().getInt("BallColor")));
    }

    public static boolean hasCarriedAnimal(Player player) {
        return player != null && player.getPersistentData().contains(CARRIED_ENTITY_TAG)
                && player.getPersistentData().contains(CARRIED_ANIMAL_TAG);
    }

    public static String carriedAnimalType(Player player) {
        return hasCarriedAnimal(player) ? player.getPersistentData().getString(CARRIED_ENTITY_TAG) : "";
    }

    public static CompoundTag carriedAnimalData(Player player) {
        return hasCarriedAnimal(player) ? player.getPersistentData().getCompound(CARRIED_ANIMAL_TAG).copy()
                : new CompoundTag();
    }

    public static void clearCarriedAnimal(Player player) {
        player.getPersistentData().remove(CARRIED_ENTITY_TAG);
        player.getPersistentData().remove(CARRIED_ANIMAL_TAG);
        if (player instanceof ServerPlayer serverPlayer) {
            com.animania.network.AnimaniaNetwork.syncCarried(serverPlayer);
        }
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
                || stack.is(AnimaniaTags.ANIMAL_FEED)
                || stack.is(AnimaniaTags.BREEDING_FOOD);
    }

    public boolean acceptsFood(ItemStack stack) { return isAnimaniaFood(stack); }
    public boolean hasInteracted() { return interacted; }
    public void markInteracted() { interacted = true; }
    public String registryPath() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id == null ? "" : id.getPath();
    }
    public String registryNamespace() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id == null ? "" : id.getNamespace();
    }

    private static boolean isAnimaniaDrink(ItemStack stack) {
        // Item tags cannot distinguish potion NBT; test PotionItem first so a
        // tag containing minecraft:potion never turns every potion into water.
        if (stack.getItem() instanceof PotionItem) return PotionUtils.getPotion(stack) == Potions.WATER;
        return stack.is(AnimaniaTags.ANIMAL_DRINK) || stack.is(Items.WATER_BUCKET)
                || stack.is(com.animania.common.AnimaniaItems.WATER_BOTTLE.get());
    }

    private static void consumeDrinkContainer(Player player, InteractionHand hand, ItemStack stack) {
        if (!stack.is(Items.WATER_BUCKET)
                && !stack.is(com.animania.common.AnimaniaItems.WATER_BOTTLE.get())
                && !(stack.getItem() instanceof PotionItem)) return;
        Item empty = stack.is(Items.WATER_BUCKET) ? Items.BUCKET : Items.GLASS_BOTTLE;
        if (stack.getCount() == 1) {
            player.setItemInHand(hand, new ItemStack(empty));
            return;
        }
        stack.shrink(1);
        ItemStack remainder = new ItemStack(empty);
        if (!player.addItem(remainder)) player.drop(remainder, false);
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
        setMateUuid(mate.getUUID());
        mate.setMateUuid(getUUID());
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
        int clamped = Math.max(0, Math.min(100, value));
        if (clamped == 100 && getHunger() < 100) fedTimer = careTimer(AnimaniaConfig.FEED_TIMER, 12000);
        entityData.set(HUNGER, clamped);
    }

    public int getFedTimer() {
        return fedTimer;
    }

    @Override
    public int getThirst() {
        return entityData.get(THIRST);
    }

    public void setThirst(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        if (clamped == 100 && getThirst() < 100) wateredTimer = careTimer(AnimaniaConfig.WATER_TIMER, 12000);
        entityData.set(THIRST, clamped);
    }

    public int getWateredTimer() {
        return wateredTimer;
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

    public int woolRegrowthTicks() {
        return Math.max(0, woolRegrowthTicks);
    }

    public int eggLayTicks() {
        return Math.max(0, eggLayTicks);
    }

    public boolean isShearableAnimal() {
        return isShearable();
    }

    public boolean isPigAnimal() {
        String path = registryPath();
        return path.startsWith("pig_") || path.startsWith("sow_") || path.startsWith("hog_")
                || path.startsWith("piglet_");
    }

    public boolean isEggLayer() {
        String path = registryPath();
        return path.startsWith("hen_") || path.startsWith("peahen_");
    }

    public void setSheared(boolean sheared) {
        entityData.set(SHEARED, sheared);
        if (!sheared) woolRegrowthTicks = 0;
    }

    @Override
    public void setPlaying(boolean playing) {
        entityData.set(PLAYING, playing);
        if (!playing) playingTicks = 0;
    }

    public boolean isMuddy() {
        return entityData.get(MUDDY);
    }

    @Override
    public int getBlinkTimer() {
        return entityData.get(BLINK_TIMER);
    }

    @Override
    public void setBlinkTimer(int ticks) {
        entityData.set(BLINK_TIMER, Math.max(-1, ticks));
    }

    public void setMuddy(boolean muddy) {
        entityData.set(MUDDY, muddy);
    }

    public int getEatingTicks() {
        return entityData.get(EATING_TICKS);
    }

    public void setEatingTicks(int ticks) {
        entityData.set(EATING_TICKS, Math.max(0, ticks));
    }

    public AnimaniaPlayGoal getPlayGoal() {
        return playGoal;
    }

    /** Called by the mud goal and collision path; never from the renderer. */
    public void enterMud() {
        if (level().isClientSide || !AnimaniaFindMudGoal.supports(this)) return;
        setMuddy(true);
        setPlaying(true);
        playingTicks = Math.max(20, config(AnimaniaConfig.PLAY_TIMER, 12000)) + random.nextInt(100);
        setSleeping(false);
    }

    private static boolean isMudBlock(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return state.is(com.animania.common.AnimaniaBlocks.MUD.get())
                || (id != null && id.getPath().equals("mud"));
    }

    /** Exact modern replacement for the legacy farm pigMudTest injection. */
    public boolean isStandingInMud() {
        if (!AnimaniaFindMudGoal.supports(this)) return false;
        BlockPos feet = blockPosition();
        return isMudBlock(level().getBlockState(feet)) || isMudBlock(level().getBlockState(feet.below()));
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
        if (!pregnant) {
            pregnancyTicks = 0;
            pregnancyDurationTicks = 0;
        } else if (pregnancyDurationTicks <= 0) {
            pregnancyDurationTicks = newPregnancyDuration();
        }
    }

    @Override
    public boolean isFertile() {
        return entityData.get(FERTILE);
    }

    @Override
    public int fertilityCooldownTicks() {
        return Math.max(0, fertilityCooldownTicks);
    }

    @Override
    public void setFertile(boolean fertile) {
        entityData.set(FERTILE, fertile && !isSterilized());
        if (fertile) fertilityCooldownTicks = 0;
    }

    @Override
    public boolean isSterilized() {
        return entityData.get(STERILIZED);
    }

    public void setSterilized(boolean sterilized) {
        entityData.set(STERILIZED, sterilized);
        if (sterilized) {
            setPregnant(false);
            setFertile(false);
        }
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

    @Nullable
    @Override
    public java.util.UUID mateUuid() {
        return entityData.get(MATE).orElse(null);
    }

    @Override
    public void setMateUuid(@Nullable java.util.UUID mateUuid) {
        entityData.set(MATE, Optional.ofNullable(mateUuid));
    }

    @Nullable
    @Override
    public java.util.UUID parentUuid() {
        return entityData.get(PARENT).orElse(null);
    }

    @Override
    public void setParentUuid(@Nullable java.util.UUID parentUuid) {
        entityData.set(PARENT, Optional.ofNullable(parentUuid));
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
        if (!ready) lactationTicks = 0;
        else if (lactationTicks <= 0) lactationTicks = childGrowthDuration();
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

    public boolean isFarmHorse() {
        return isHorseAnimal();
    }

    /** Legacy AI schedules treat ticks 0..11999 as day without relying on the cached sky-darkness value. */
    public boolean isLegacyDaytime() {
        return Math.floorMod(level().getDayTime(), 24000L) < 12000L;
    }

    /** True while a nearby native cart/wagon/tiller has this horse as its synchronized puller. */
    public boolean isPullingVehicle() {
        if (!isHorseAnimal()) return false;
        return level().getEntitiesOfClass(AnimaniaVehicleEntity.class, getBoundingBox().inflate(3.5D))
                .stream().anyMatch(vehicle -> vehicle.getPuller() == this);
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
        fedTimer = careTimer(AnimaniaConfig.FEED_TIMER, 12000);
        setHunger(Math.min(100, getHunger() + 20));
        if (isAdult() && !isSterilized()) {
            setInLove(null);
        }
        return true;
    }

    /** Exact modern predicate for the legacy 60-tick unhappy smoke cue. */
    public boolean shouldShowUnhappyParticles() {
        return config(AnimaniaConfig.SHOW_UNHAPPY_PARTICLES, true)
                && getHunger() <= 0 && getThirst() <= 0 && !isSleeping()
                && (!config(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) || hasInteracted());
    }

    @Override
    public boolean drink(ItemStack stack) {
        if (stack == null || stack.isEmpty() || level().isClientSide || !isAnimaniaDrink(stack)) return false;
        interacted = true;
        wateredTimer = careTimer(AnimaniaConfig.WATER_TIMER, 12000);
        setThirst(100);
        return true;
    }

    @Override
    public boolean play(ItemStack stack) {
        if (stack == null || stack.isEmpty() || level().isClientSide || !isCompanionAnimal()) return false;
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
        if (getHunger() <= 0 || getThirst() <= 0 || mate.getHunger() <= 0 || mate.getThirst() <= 0) return false;
        if (getAge() != 0 || mate.getAge() != 0 || isSleeping() || mate.isSleeping()
                || isInWater() || mate.isInWater()) return false;
        if (!breedingCapacityAvailable(mate)) return false;
        return sameSpecies(mate)
                && isAdult() && mate.isAdult()
                && getGender() != mate.getGender()
                && !isSterilized() && !mate.isSterilized()
                && (getGender() != AnimalGender.FEMALE || isFertile())
                && (mate.getGender() != AnimalGender.FEMALE || mate.isFertile())
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

    /** Select the same coat/colour families that were stored as 1.12 integer variants. */
    private String initialVariant() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return "default";
        String path = id.getPath();
        String[] variants = null;
        if (path.endsWith("_draft")) variants = new String[]{"black", "bw1", "bw2", "grey", "red", "white"};
        else if (path.equals("hamster")) variants = new String[]{"black", "brown", "darkbrown", "darkgray", "gray", "plum", "tarou", "white", "gold"};
        else if (path.equals("dartfrog")) variants = new String[]{"blue", "red", "yellow"};
        else if (path.equals("frog")) variants = new String[]{"default", "green"};
        else if (path.endsWith("_chihuahua") || path.endsWith("_collie")) variants = new String[]{"0", "1"};
        else if (path.endsWith("_labrador") || path.endsWith("_poodle")) variants = new String[]{"0", "1", "2"};
        else if (path.endsWith("_wolf")) variants = new String[]{"0", "1", "2", "3", "4", "5", "6", "7"};
        else if (path.endsWith("_lop")) variants = new String[]{"black", "brown", "golden", "olive", "patch_black", "patch_brown", "patch_grey"};
        else if (path.endsWith("_dorset") || path.endsWith("_merino") || path.endsWith("_suffolk")) variants = new String[]{"white", "brown"};
        else if (path.endsWith("_friesian") && (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_"))) {
            variants = new String[]{"white", "black", "brown"};
        }
        return variants == null ? "default" : variants[random.nextInt(variants.length)];
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

    private int careTimer(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        return Math.max(1, config(value, fallback)) + random.nextInt(100);
    }

    private static double config(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    /** 1.12 children advance from 0.00 to 0.85 in 0.01 care-gated steps. */
    public static int childGrowthDuration() {
        return Math.multiplyExact(85, childGrowthInterval());
    }

    private static int childGrowthInterval() {
        return Math.max(20, config(AnimaniaConfig.CHILD_GROWTH_TICK, 200));
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
        tag.putInt("AnimaniaPlayingTicks", playingTicks);
        tag.putBoolean("AnimaniaMuddy", isMuddy());
        tag.putBoolean("AnimaniaPregnant", isPregnant());
        tag.putBoolean("AnimaniaSterilized", isSterilized());
        tag.putBoolean("AnimaniaSheared", isSheared());
        tag.putBoolean("AnimaniaTamed", isTamed());
        tag.putBoolean("AnimaniaSitting", isSitting());
        tag.putBoolean("AnimaniaSaddled", isSaddled());
        if (getOwnerUUID() != null) tag.putUUID("AnimaniaOwner", getOwnerUUID());
        tag.putInt("AnimaniaPregnancyTicks", pregnancyTicks);
        tag.putInt("AnimaniaPregnancyDuration", pregnancyDurationTicks);
        tag.putBoolean("Fertile", isFertile());
        tag.putInt("AnimaniaFertilityCooldown", fertilityCooldownTicks);
        tag.putInt("AnimaniaLactationTicks", lactationTicks);
        tag.putInt("AnimaniaWoolRegrowthTicks", woolRegrowthTicks);
        tag.putInt("AnimaniaBoostTicks", boostTicks);
        tag.putInt("AnimaniaStarvationTicks", starvationTicks);
        tag.putInt("AnimaniaEggLayTicks", eggLayTicks);
        tag.putInt("AnimaniaFeatherDropTicks", featherDropTicks);
        tag.putInt("AnimaniaFedTimer", fedTimer);
        tag.putInt("AnimaniaWateredTimer", wateredTimer);
        tag.putInt("AnimaniaChildGrowthTimer", childGrowthTimer);
        tag.putInt("CrowTime", crowCooldown);
        tag.putBoolean("AnimaniaMilkReady", isMilkReady());
        tag.putBoolean("AnimaniaInteracted", interacted);
        tag.putBoolean("AnimaniaInBall", isInBall());
        tag.putBoolean("InBall", isInBall());
        tag.putInt("AnimaniaBallColor", getBallColor());
        tag.putInt("BallColor", getBallColor());
        if (isHamster()) {
            tag.putInt("AnimaniaHamsterFoodStack", getHamsterFoodStack());
            tag.putInt("foodStackCount", getHamsterFoodStack());
            tag.putBoolean("AnimaniaHamsterStanding", isHamsterStanding());
            tag.putInt("AnimaniaHamsterStandTicks", hamsterStandTicks);
            tag.putInt("AnimaniaHamsterEatTicks", hamsterEatTicks);
        }
        tag.putInt("AnimaniaWoolColor", getWoolColor());
        tag.putInt("DyeColor", getWoolColor());
        tag.putInt("AnimaniaBlinkTimer", getBlinkTimer());
        tag.putBoolean("AnimaniaSpooked", isSpooked());
        tag.putInt("AnimaniaSpookedTimer", getSpookedTimer());
        tag.putBoolean("AnimaniaFighting", isFighting());
        tag.putInt("AnimaniaFightTimer", getFightTimer());
        if (getRivalUuid() != null) tag.putUUID("AnimaniaRival", getRivalUuid());
        if (mateUuid() != null) tag.putUUID("MateUUID", mateUuid());
        if (parentUuid() != null) tag.putUUID("ParentUUID", parentUuid());
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
        playingTicks = Math.max(0, tag.getInt("AnimaniaPlayingTicks"));
        if (isPlaying() && playingTicks == 0 && AnimaniaFindMudGoal.supports(this)) {
            playingTicks = Math.max(20, config(AnimaniaConfig.PLAY_TIMER, 12000));
        } else if (isPlaying() && playingTicks == 0) {
            setPlaying(false);
        }
        setMuddy(tag.getBoolean("AnimaniaMuddy"));
        setPregnant(tag.getBoolean("AnimaniaPregnant"));
        setSterilized(tag.getBoolean("AnimaniaSterilized"));
        setSheared(tag.getBoolean("AnimaniaSheared"));
        setTamed(tag.getBoolean("AnimaniaTamed"));
        if (tag.hasUUID("AnimaniaOwner")) setOwnerUUID(tag.getUUID("AnimaniaOwner"));
        setSitting(tag.getBoolean("AnimaniaSitting"));
        setSaddled(tag.getBoolean("AnimaniaSaddled"));
        pregnancyTicks = tag.getInt("AnimaniaPregnancyTicks");
        pregnancyDurationTicks = tag.contains("AnimaniaPregnancyDuration")
                ? Math.max(0, tag.getInt("AnimaniaPregnancyDuration"))
                : isPregnant() ? newPregnancyDuration() : 0;
        fertilityCooldownTicks = Math.max(0, tag.getInt("AnimaniaFertilityCooldown"));
        setFertile(!tag.contains("Fertile") || tag.getBoolean("Fertile"));
        if (tag.contains("AnimaniaLactationTicks")) {
            lactationTicks = Math.max(0, tag.getInt("AnimaniaLactationTicks"));
        }
        woolRegrowthTicks = Math.max(0, tag.getInt("AnimaniaWoolRegrowthTicks"));
        boostTicks = Math.max(0, tag.getInt("AnimaniaBoostTicks"));
        starvationTicks = Math.max(0, tag.getInt("AnimaniaStarvationTicks"));
        eggLayTicks = Math.max(0, tag.getInt("AnimaniaEggLayTicks"));
        featherDropTicks = Math.max(0, tag.getInt("AnimaniaFeatherDropTicks"));
        fedTimer = tag.contains("AnimaniaFedTimer") ? Math.max(0, tag.getInt("AnimaniaFedTimer"))
                : careTimer(AnimaniaConfig.FEED_TIMER, 12000);
        wateredTimer = tag.contains("AnimaniaWateredTimer") ? Math.max(0, tag.getInt("AnimaniaWateredTimer"))
                : careTimer(AnimaniaConfig.WATER_TIMER, 12000);
        childGrowthTimer = tag.contains("AnimaniaChildGrowthTimer")
                ? Math.max(0, tag.getInt("AnimaniaChildGrowthTimer")) : 0;
        crowCooldown = Math.max(0, tag.getInt("CrowTime"));
        setMilkReady(tag.getBoolean("AnimaniaMilkReady"));
        interacted = tag.getBoolean("AnimaniaInteracted");
        setInBall(tag.getBoolean("AnimaniaInBall") || tag.getBoolean("InBall"));
        setBallColor(tag.contains("AnimaniaBallColor") ? tag.getInt("AnimaniaBallColor") : tag.getInt("BallColor"));
        if (isHamster()) {
            setHamsterFoodStack(tag.contains("AnimaniaHamsterFoodStack")
                    ? tag.getInt("AnimaniaHamsterFoodStack") : tag.getInt("foodStackCount"));
            setHamsterStanding(tag.getBoolean("AnimaniaHamsterStanding"),
                    Math.max(1, tag.getInt("AnimaniaHamsterStandTicks")));
            hamsterEatTicks = tag.contains("AnimaniaHamsterEatTicks")
                    ? Math.max(0, tag.getInt("AnimaniaHamsterEatTicks")) : 5000;
        }
        setWoolColor(tag.contains("AnimaniaWoolColor") ? tag.getInt("AnimaniaWoolColor")
                : tag.contains("DyeColor") ? tag.getInt("DyeColor") : DyeColor.WHITE.getId());
        setBlinkTimer(tag.contains("AnimaniaBlinkTimer") ? tag.getInt("AnimaniaBlinkTimer") : getBlinkTimer());
        setSpooked(tag.getBoolean("AnimaniaSpooked"));
        setSpookedTimer(Math.max(0, tag.getInt("AnimaniaSpookedTimer")));
        setFighting(tag.getBoolean("AnimaniaFighting"));
        setFightTimer(Math.max(0, tag.getInt("AnimaniaFightTimer")));
        setRivalUuid(tag.hasUUID("AnimaniaRival") ? tag.getUUID("AnimaniaRival") : null);
        setMateUuid(tag.hasUUID("MateUUID") ? tag.getUUID("MateUUID")
                : tag.hasUUID("AnimaniaMate") ? tag.getUUID("AnimaniaMate") : null);
        setParentUuid(tag.hasUUID("ParentUUID") ? tag.getUUID("ParentUUID")
                : tag.hasUUID("AnimaniaParent") ? tag.getUUID("AnimaniaParent") : null);
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float multiplier) {
        int damage = super.calculateFallDamage(fallDistance, multiplier);
        double reduction = Math.max(0.0D, Math.min(1.0D, config(AnimaniaConfig.FALL_DAMAGE_REDUCE_MULTIPLIER, 0.45D)));
        return legacyFallDamage(damage, isLeashed(), reduction);
    }

    public static int legacyFallDamage(int damage, boolean leashed, double reduction) {
        if (!leashed) return Math.max(0, damage);
        double clamped = Math.max(0.0D, Math.min(1.0D, reduction));
        return net.minecraft.util.Mth.floor(Math.max(0, damage) * clamped);
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
        if (id == null || !"animania_farm".equals(id.getNamespace())) return false;
        String path = id.getPath();
        // 1.12 allowed adult ewes/rams of every sheep breed and only adult
        // Angora bucks/does.  Other goat breeds must never acquire a sheared
        // state (which also prevents impossible texture paths and wool dupes).
        return path.startsWith("ewe_") || path.startsWith("ram_")
                || path.equals("buck_angora") || path.equals("doe_angora");
    }

    private Item woolDropItem() {
        DyeColor color = DyeColor.byId(getWoolColor());
        Item colored = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", color.getName() + "_wool"));
        return colored == null || colored == Items.AIR ? Items.WHITE_WOOL : colored;
    }

    private void produceFeather() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null) return;
        String path = id.getPath();
        Item item = null;
        if (path.startsWith("hen_") || path.startsWith("rooster_")) {
            item = Items.FEATHER;
        } else if (path.startsWith("peacock_")) {
            String color = speciesKey(path);
            item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_extra", color + "_peacock_feather"));
        }
        if (item != null) spawnAtLocation(new ItemStack(item));
    }

    private boolean canDropFeather() {
        String path = registryPath();
        return path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("peacock_");
    }

    private int nextFeatherDropTicks() {
        int base = Math.max(20, config(AnimaniaConfig.FEATHER_TIMER, 12000));
        String path = registryPath();
        return path.startsWith("hen_") || path.startsWith("rooster_") ? base + random.nextInt(1000) : base;
    }

    private int nextWoolRegrowthTicks() {
        return Math.max(20, config(AnimaniaConfig.WOOL_REGROWTH_TIMER, 8000)) + random.nextInt(500);
    }

    /**
     * Farm addon hook for the legacy hen laying rule. The countdown is kept
     * on the entity and persisted, so a chunk unload cannot duplicate eggs.
     */
    public boolean tryLayFarmEgg(boolean enabled) {
        if (level().isClientSide || !isAdult() || getGender() != AnimalGender.FEMALE) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_farm".equals(id.getNamespace()) || !id.getPath().startsWith("hen_")) return false;
        if (eggLayTicks <= 0) eggLayTicks = Math.max(20, config(AnimaniaConfig.LAID_TIMER, 2000)) + random.nextInt(100);
        if (--eggLayTicks > 0) return false;
        eggLayTicks = Math.max(20, config(AnimaniaConfig.LAID_TIMER, 2000)) + random.nextInt(100);
        String variant = speciesKey(id.getPath());
        boolean brown = variant.equals("rhode_island_red") || variant.equals("wyandotte");
        Item egg = brown ? ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_farm", "brown_egg")) : Items.EGG;
        if (egg == null || egg == Items.AIR) return false;
        BlockPos origin = blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-10, -3, -10), origin.offset(10, 3, 10))) {
            if (level().getBlockEntity(candidate) instanceof com.animania.common.AnimaniaBlocks.NestEntity nest
                    && nest.insertEgg(new ItemStack(egg), variant)) return true;
        }
        if (!enabled) { eggLayTicks = 1; return false; }
        spawnAtLocation(new ItemStack(egg));
        return true;
    }

    /** Extra addon hook: peahens retain the 1.12 blue/white, nest-only laying rule. */
    public boolean tryLayPeafowlEgg() {
        if (level().isClientSide || !isAdult() || getGender() != AnimalGender.FEMALE) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_extra".equals(id.getNamespace()) || !id.getPath().startsWith("peahen_")) return false;
        if (eggLayTicks <= 0) eggLayTicks = Math.max(20, config(AnimaniaConfig.LAID_TIMER, 2000)) + random.nextInt(100);
        if (--eggLayTicks > 0) return false;
        String variant = speciesKey(id.getPath());
        String eggId = variant.equals("blue") ? "peacock_egg_blue" : "peacock_egg_white";
        Item egg = ForgeRegistries.ITEMS.getValue(new ResourceLocation("animania_extra", eggId));
        if (egg == null || egg == Items.AIR) { eggLayTicks = 1; return false; }
        BlockPos origin = blockPosition();
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-10, -3, -10), origin.offset(10, 3, 10))) {
            if (level().getBlockEntity(candidate) instanceof com.animania.common.AnimaniaBlocks.NestEntity nest
                    && nest.insertEgg(new ItemStack(egg), variant)) {
                eggLayTicks = Math.max(20, config(AnimaniaConfig.LAID_TIMER, 2000)) + random.nextInt(100);
                return true;
            }
        }
        eggLayTicks = 1;
        return false;
    }

    /** Farm addon hook enabling the optional rooster-vs-rooster target goal. */
    public void configureRoosterCombat(boolean enabled) {
        if (!enabled || roosterCombatConfigured || level().isClientSide) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        if (id == null || !"animania_farm".equals(id.getNamespace()) || !id.getPath().startsWith("rooster_")) return;
        targetSelector.addGoal(5, new AnimaniaNearestAttackableTargetGoal<>(this, AnimaniaAnimalEntity.class, true,
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

    /**
     * Compatibility conversion used by the guarded {@code /animania
     * tovanilla} command.  It intentionally creates a fresh vanilla entity
     * and leaves removal/spawning to the command, so a failed conversion can
     * never delete the source animal.
     */
    @Override
    @Nullable
    public Entity convertToVanilla() {
        EntityType<?> vanillaType = vanillaConversionType();
        if (vanillaType == null) return null;
        Entity converted = vanillaType.create(level());
        if (converted == null) return null;
        converted.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        converted.setDeltaMovement(getDeltaMovement());
        if (hasCustomName()) converted.setCustomName(getCustomName());
        converted.setCustomNameVisible(isCustomNameVisible());
        if (converted instanceof AgeableMob ageable && isBaby()) ageable.setBaby(true);
        if (converted instanceof net.minecraft.world.entity.animal.Sheep sheep) {
            sheep.setColor(DyeColor.byId(getWoolColor()));
            sheep.setSheared(isSheared());
        }
        if (converted instanceof net.minecraft.world.entity.Saddleable saddleable && isSaddled()) {
            saddleable.equipSaddle(null);
        }
        if (converted instanceof net.minecraft.world.entity.TamableAnimal tame && isTamed()) {
            tame.setTame(true);
            tame.setOwnerUUID(getOwnerUUID());
            if (tame instanceof net.minecraft.world.entity.animal.Wolf wolf) wolf.setOrderedToSit(isSitting());
            if (tame instanceof net.minecraft.world.entity.animal.Cat cat) cat.setOrderedToSit(isSitting());
        }
        return converted;
    }

    /** Registry-independent family mapping used by conversion and its tests. */
    @Nullable
    public EntityType<?> vanillaConversionType() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return vanillaTypeFor(id);
    }

    /** Pure mapping helper used by migration tests and third-party converters. */
    @Nullable
    public static EntityType<?> vanillaTypeFor(@Nullable ResourceLocation id) {
        ResourceLocation vanillaId = vanillaTypeIdFor(id);
        return vanillaId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(vanillaId);
    }

    /** Pure registry-ID mapping that is safe to use during data-generation tests. */
    @Nullable
    public static ResourceLocation vanillaTypeIdFor(@Nullable ResourceLocation id) {
        return com.animania.common.command.AnimaniaConversion.vanillaTypeIdFor(id);
    }
}
