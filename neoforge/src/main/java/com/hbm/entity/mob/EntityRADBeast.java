// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemZirnoxRod.EnumZirnoxType;
import com.hbm.items.special.Autogen;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.util.MobUtil;
import java.util.EnumSet;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityRADBeast extends Monster implements IRadiationImmune {

    private static final EntityDataAccessor<Integer> VICTIM =
            SynchedEntityData.defineId(EntityRADBeast.class, EntityDataSerializers.INT);

    private static final double LEADER_HEALTH = 360.0D;

    private float heightOffset = 0.5F;
    private int heightOffsetUpdateTime;

    private int attackDelay;

    public EntityRADBeast(EntityType<? extends EntityRADBeast> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, -1.0F);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(Attributes.MOVEMENT_SPEED.value().getDefaultValue()))
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VICTIM, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(4, new RadiationAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public EntityRADBeast makeLeader() {
        setDropChance(EquipmentSlot.MAINHAND, 1F);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COIN_RADIATION.get()));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(LEADER_HEALTH);
        heal(getMaxHealth());
        return this;
    }

    public boolean isLeader() {
        return getMaxHealth() > 150;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    public @Nullable Entity getUnfortunateSoul() {
        int id = entityData.get(VICTIM);
        return id == 0 ? null : level().getEntity(id);
    }

    @Override
    public void aiStep() {
        if (level() instanceof ServerLevel server) {
            if (isInWaterOrRain()) hurtServer(server, damageSources().drown(), 1.0F);

            if (--this.heightOffsetUpdateTime <= 0) {
                this.heightOffsetUpdateTime = 100;
                this.heightOffset = 0.5F + (float) random.nextGaussian() * 3.0F;
            }

            LivingEntity target = getTarget();

            if (target != null
                    && target.getY() + target.getEyeHeight()
                            > getY() + getEyeHeight() + this.heightOffset) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x, motion.y + (0.3D - motion.y) * 0.3D, motion.z);
            }

            entityData.set(VICTIM, target != null && this.attackDelay < 10 ? target.getId() : 0);
        }

        if (!onGround() && getDeltaMovement().y < 0) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
        }

        spawnHeatParticles();
        super.aiStep();
    }

    private void spawnHeatParticles() {
        if (isLeader()) {
            level().addParticle(
                            ParticleTypes.LAVA,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            getY() + random.nextDouble() * getBbHeight() * 0.75,
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            0,
                            0,
                            0);
            return;
        }

        for (int i = 0; i < 6; i++) {
            level().addParticle(
                            ParticleTypes.MYCELIUM,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5,
                            getY() + random.nextDouble() * getBbHeight(),
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5,
                            0,
                            0,
                            0);
        }

        if (random.nextInt(6) == 0) {
            level().addParticle(
                            ParticleTypes.FLAME,
                            getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            getY() + random.nextDouble() * getBbHeight() * 0.75,
                            getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                            0,
                            0,
                            0);
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GEIGER[random.nextInt(ModSounds.GEIGER.length)].get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STEP_METAL.get();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isLeader()) HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.MELTDOWN, 50D);
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!killedByPlayer) return;

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            Holder<Enchantment> lootingEnchant =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getEnchantmentLevel(lootingEnchant, attacker);
        }

        if (looting > 0) {
            spawnAtLocation(
                    level,
                    new ItemStack(
                            Autogen.require(MaterialShapes.NUGGET, Mats.MAT_POLONIUM), looting));
        }

        boolean wet = isInWaterOrRain();
        int count = random.nextInt(3) + 1;

        for (int i = 0; i < count; i++) {
            EnumZirnoxType rod =
                    switch (random.nextInt(3)) {
                        case 0 -> EnumZirnoxType.URANIUM_FUEL;
                        case 1 -> EnumZirnoxType.MOX_FUEL;
                        default -> EnumZirnoxType.PLUTONIUM_FUEL;
                    };

            spawnAtLocation(
                    level,
                    wet
                            ? new ItemStack(wasteFor(rod), 2)
                            : new ItemStack(ModItems.zirnoxDepleted(rod)));
        }
    }

    private static Item wasteFor(EnumZirnoxType rod) {
        return switch (rod) {
            case MOX_FUEL -> ModItems.WASTE_MOX.get();
            case PLUTONIUM_FUEL -> ModItems.WASTE_PLUTONIUM.get();
            default -> ModItems.WASTE_URANIUM.get();
        };
    }

    private static class RadiationAttackGoal extends Goal {

        private static final double PULSE_RANGE = 30.0D;
        private static final double MELEE_RANGE = 2.0D;

        private final EntityRADBeast beast;

        RadiationAttackGoal(EntityRADBeast beast) {
            this.beast = beast;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.beast.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.beast.getTarget();
            if (target == null) return;

            if (this.beast.attackDelay > 0) this.beast.attackDelay--;

            this.beast.getLookControl().setLookAt(target, 30F, 30F);

            if (this.beast.getNavigation().isDone() || this.beast.getRandom().nextInt(20) == 0) {
                this.beast.getNavigation().moveTo(target, 1.0D);
            }

            if (this.beast.attackDelay > 0) return;

            double distance = this.beast.distanceTo(target);
            AABB self = this.beast.getBoundingBox();
            AABB other = target.getBoundingBox();

            if (distance < MELEE_RANGE && other.maxY > self.minY && other.minY < self.maxY) {
                if (this.beast.level() instanceof ServerLevel server)
                    this.beast.doHurtTarget(server, target);
                this.beast.attackDelay = 20;
                return;
            }

            if (distance >= PULSE_RANGE || !(this.beast.level() instanceof ServerLevel server))
                return;

            RadiationSystemNT.incrementRad(server, this.beast.blockPosition(), 100);
            target.hurtServer(
                    server, server.damageSources().source(ModDamageTypes.RADIATION), 16.0F);
            this.beast.swing(InteractionHand.MAIN_HAND);
            this.beast.playAmbientSound();
            this.beast.attackDelay = 20;
        }
    }
}
