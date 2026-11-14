/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.entity.mob;

import com.hbm.api.environment.SuffocationImmune;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.BulletEntity;
import com.hbm.entity.projectile.ChopperMineEntity;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.EntityAudioLoop;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.MobUtil;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HunterChopperEntity extends FlyingMobEntity implements Enemy, SuffocationImmune {

    public static final String HIT_LOOT = "hit";
    public static final String CRASH_LOOT = "crash";

    private static final EntityDataAccessor<Byte> ATTACKING =
            SynchedEntityData.defineId(HunterChopperEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DYING =
            SynchedEntityData.defineId(HunterChopperEntity.class, EntityDataSerializers.BYTE);

    private static final List<ResourceKey<DamageType>> FULL_DAMAGE =
            List.of(
                    ModDamageTypes.SHRAPNEL,
                    ModDamageTypes.NUCLEAR_BLAST,
                    ModDamageTypes.BLACKHOLE,
                    ModDamageTypes.TAU,
                    ModDamageTypes.SUBATOMIC);

    private static final double TARGET_RANGE = 250D;
    private static final double GUN_RANGE = 64D;

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS);

    private double waypointX;
    private double waypointY;
    private double waypointZ;

    private int courseChangeCooldown;
    private int attackCounter;
    private int mineDropCounter;
    private @Nullable Entity targetedEntity;
    private final EntityAudioLoop flyingLoop =
            new EntityAudioLoop(
                    this, ModSounds.CHOPPER_FLYING_LOOP, SoundSource.HOSTILE, 10F, 160F);
    private final EntityAudioLoop crashingLoop =
            new EntityAudioLoop(
                    this, ModSounds.CHOPPER_CRASHING_LOOP, SoundSource.HOSTILE, 10F, 160F);

    public HunterChopperEntity(EntityType<? extends HunterChopperEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 750.0D)
                .add(Attributes.FOLLOW_RANGE, 250.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, (byte) 0);
        builder.define(DYING, (byte) 0);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public boolean isDying() {
        return entityData.get(DYING) == 1;
    }

    private void setDying(boolean dying) {
        entityData.set(DYING, (byte) (dying ? 1 : 0));
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING) != 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean exotic =
                source.is(DamageTypeTags.IS_EXPLOSION) || FULL_DAMAGE.stream().anyMatch(source::is);

        if (!exotic) amount *= 0.1F;

        if (isInvulnerableTo(level, source)
                || source.getDirectEntity() != null
                || getHealth() <= 0.1F) {
            return false;
        }

        if (amount >= getHealth()) {
            initDeath(level);
            setDying(true);
            setHealth(0.1F);
            return false;
        }

        if (!isDying() && random.nextInt(15) == 0) {
            level.explode(this, getX(), getY(), getZ(), 5F, Level.ExplosionInteraction.BLOCK);
            dropFromGiftLootTable(
                    level, MobUtil.lootVariant(getType(), HIT_LOOT), this::spawnAtLocation);
        }

        for (int i = 0; i < 8; i++) {
            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    getX(),
                    getY(),
                    getZ(),
                    3,
                    i * 0.05D,
                    i * 0.05D,
                    i * 0.05D,
                    0.05D);
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (isDying()) {
            crashStep(level);
        } else {
            flyStep(level);
        }

        aimAt(this.targetedEntity);
    }

    private void flyStep(ServerLevel level) {
        double dx = this.waypointX - getX();
        double dy = this.waypointY - getY();
        double dz = this.waypointZ - getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < 1.0D || distSq > 3600.0D) pickWaypoint(this.targetedEntity);

        if (this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown += random.nextInt(5) + 2;
            double dist = Math.sqrt(distSq);

            if (canReach(new Vec3(dx, dy, dz))) {
                setDeltaMovement(
                        getDeltaMovement()
                                .add(dx / dist * 0.1D, dy / dist * 0.1D, dz / dist * 0.1D));
            } else {
                pickWaypoint(null);
            }
        }

        if (this.targetedEntity != null
                && (!this.targetedEntity.isAlive() || this.targetedEntity.isSpectator())) {
            this.targetedEntity = null;
        }
        if (this.targetedEntity == null || this.attackCounter <= 0)
            this.targetedEntity = findTarget();

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < GUN_RANGE * GUN_RANGE) {
            engage(level, this.targetedEntity);
        } else if (this.attackCounter > 0) {
            this.attackCounter = 0;
        }

        entityData.set(ATTACKING, (byte) (this.attackCounter > 10 ? 1 : 0));
    }

    private void engage(ServerLevel level, Entity target) {
        Vec3 look = getLookAngle();
        double startX = getX() + look.x * 2D;
        double startY = getY() - 0.5D;
        double startZ = getZ() + look.z * 2D;

        if (++this.attackCounter >= 200) this.attackCounter -= 200;

        if (this.attackCounter == 80) {
            WorldSounds.play(
                    level,
                    null,
                    this,
                    ModSounds.CHOPPER_CHARGE.get(),
                    SoundSource.HOSTILE,
                    5.0F,
                    1.0F);
        }

        if (this.attackCounter >= 120 && this.attackCounter % 2 == 0) {
            Vec3 heading =
                    new Vec3(
                                    target.getX() - startX - 1 + random.nextInt(3),
                                    target.getBoundingBox().minY
                                            + target.getBbHeight() / 2F
                                            - startY
                                            - 1
                                            + random.nextInt(3),
                                    target.getZ() - startZ - 1 + random.nextInt(3))
                            .normalize()
                            .scale(3D);

            BulletEntity bullet =
                    new BulletEntity(
                            ModEntities.BULLET.get(), level, this, 3.0F, BulletEntity.Mode.CHOPPER);
            bullet.setPos(startX, startY, startZ);
            bullet.setDeltaMovement(heading);
            bullet.setDamage(3 + random.nextInt(5));
            level.addFreshEntity(bullet);
        }

        if (++this.mineDropCounter > 100 && random.nextInt(15) == 0) {
            this.mineDropCounter = 0;
            WorldSounds.play(
                    level,
                    null,
                    this,
                    ModSounds.CHOPPER_DROP.get(),
                    SoundSource.HOSTILE,
                    15.0F,
                    1.0F);
            dropMine(level, 0, 0);

            if (random.nextInt(3) == 0) {
                dropMine(level, 1, 0);
                dropMine(level, -1, 0);
                dropMine(level, 0, 1);
                dropMine(level, 0, -1);
            }
        }
    }

    private void dropMine(ServerLevel level, double motionX, double motionZ) {
        level.addFreshEntity(
                new ChopperMineEntity(
                        level, getX(), getY() - 0.5D, getZ(), motionX, -0.3D, motionZ, this));
    }

    private void crashStep(ServerLevel level) {
        setDeltaMovement(getDeltaMovement().add(0D, -0.08D, 0D));

        Vec3 motion = getDeltaMovement();
        if (Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 1.2D < 1.8D) {
            setDeltaMovement(motion.x * 1.2D, motion.y, motion.z * 1.2D);
        }

        if (random.nextInt(20) == 0) {
            level.explode(this, getX(), getY(), getZ(), 5F, Level.ExplosionInteraction.BLOCK);
        }

        level.sendParticles(
                HbmParticles.LAUNCH_SMOKE.get(), getX(), getY(), getZ(), 10, 1D, 1D, 1D, 0.05D);

        setYRot(getYRot() + 20F);

        if (onGround()) {
            level.explode(this, getX(), getY(), getZ(), 15F, Level.ExplosionInteraction.BLOCK);
            dropFromGiftLootTable(
                    level, MobUtil.lootVariant(getType(), CRASH_LOOT), this::spawnAtLocation);
            discard();
        }
    }

    private @Nullable Entity findTarget() {
        AABB box = getBoundingBox().inflate(TARGET_RANGE, TARGET_RANGE / 2D, TARGET_RANGE);
        Entity closest = null;
        double closestSq = -1D;

        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!candidate.isAlive() || candidate instanceof HunterChopperEntity) continue;
            if (candidate instanceof Player player && player.getAbilities().invulnerable) continue;

            double distSq = candidate.distanceToSqr(this);
            double range = candidate.isShiftKeyDown() ? TARGET_RANGE * 0.8D : TARGET_RANGE;

            if (distSq < range * range && (closestSq == -1D || distSq < closestSq)) {
                closestSq = distSq;
                closest = candidate;
            }
        }

        return closest;
    }

    private void pickWaypoint(@Nullable Entity around) {
        double originX = around != null ? around.getX() : getX();
        double originZ = around != null ? around.getZ() : getZ();

        this.waypointX = originX + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        this.waypointZ = originZ + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
        this.waypointY =
                surfaceAt((int) this.waypointX, (int) this.waypointZ) + 10 + random.nextInt(15);
    }

    private void aimAt(@Nullable Entity target) {
        Vec3 motion = getDeltaMovement();
        double bearing =
                target != null
                        ? Math.atan2(getX() - target.getX(), getZ() - target.getZ())
                        : Math.atan2(motion.x, motion.z);
        float wanted = (float) (bearing * 180D / Math.PI);
        float delta = getYRot() - wanted;

        this.yRotO = getYRot();
        if (delta >= 10F) setYRot(getYRot() - 10F);
        else if (delta <= -10F) setYRot(getYRot() + 10F);
        this.yRotO = getYRot();

        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setXRot((float) (Math.atan2(motion.y, horizontal) * 180D / Math.PI));
        this.xRotO = getXRot();

        if (getXRot() <= 330F && getXRot() >= 30F) setXRot(getXRot() < 180F ? 30F : 330F);
    }

    private void initDeath(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 10F, Level.ExplosionInteraction.BLOCK);
        if (!isDying()) {
            WorldSounds.play(
                    level,
                    null,
                    this,
                    ModSounds.CHOPPER_DAMAGE.get(),
                    SoundSource.HOSTILE,
                    10.0F,
                    1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            boolean dying = isDying();
            this.flyingLoop.tick(!dying);
            this.crashingLoop.tick(dying);
            return;
        }

        WorldSounds.entityLoop(this, tickCount == 1);
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.flyingLoop.stop();
        this.crashingLoop.stop();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }
}
