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

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.environment.SuffocationImmune;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class UFOEntity extends FlyingMobEntity implements Enemy, SuffocationImmune {

    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(UFOEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.GREEN,
                    BossEvent.BossBarOverlay.PROGRESS);

    private final List<Entity> secondaries = new ArrayList<>();

    public int courseChangeCooldown;
    public int scanCooldown;
    public int hurtCooldown;
    public int beamTimer;

    private @Nullable Entity target;

    public UFOEntity(EntityType<? extends UFOEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.deathTime = -30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20000.0D)
                .add(Attributes.FOLLOW_RANGE, 150.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM, false);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (this.hurtCooldown > 0) return false;

        boolean hit = super.hurtServer(level, source, amount);
        if (hit) this.hurtCooldown = 5;
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (this.target != null && (!this.target.isAlive() || this.target.isSpectator()))
            this.target = null;

        if (this.scanCooldown <= 0) scanForTargets();
        if (this.target != null && this.courseChangeCooldown <= 0) setCourse(this.target);

        tickBeam(level);
        tickWeapons();
        approachWaypoint();
    }

    private void scanForTargets() {

        this.secondaries.clear();
        this.target = null;

        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(100D, 50D, 100D))) {

            if (!entity.isAlive() || !canTarget(entity)) continue;

            if (entity instanceof Player player) {
                if (player.isCreative()) continue;
                if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

                if (this.target == null || distanceToSqr(player) < distanceToSqr(this.target))
                    this.target = player;
            }

            if (entity instanceof LivingEntity living
                    && distanceToSqr(living) < 100D * 100D
                    && getSensing().hasLineOfSight(living)
                    && living != this.target) {
                this.secondaries.add(living);
            }
        }

        if (this.target == null && !this.secondaries.isEmpty()) {
            this.target = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
        }

        this.scanCooldown = 50;
    }

    private void setCourse(Entity prey) {
        Vec3 bearing = offsetFrom(prey);
        if (this.random.nextInt(3) > 0)
            bearing = bearing.yRot((float) Math.PI * 2F * this.random.nextFloat());
        setWaypointPast(
                prey,
                bearing,
                35D,
                (surface, preyY) -> Math.max(surface + 20 + this.random.nextInt(15), preyY + 15));
        this.courseChangeCooldown = 40 + this.random.nextInt(20);
    }

    private void tickBeam(ServerLevel level) {

        if (this.beamTimer <= 0 && getBeam()) setBeam(false);

        if (this.target != null) {
            double dist =
                    Math.abs(this.target.getX() - getX()) + Math.abs(this.target.getZ() - getZ());
            if (dist < 25D) this.beamTimer = 30;
        }

        if (this.beamTimer <= 0) return;
        this.beamTimer--;

        if (!getBeam()) {
            WorldSounds.play(
                    level, null, this, ModSounds.UFO_BEAM.get(), SoundSource.HOSTILE, 10F, 1F);
            setBeam(true);
        }

        int groundY = groundBelow();
        if (groundY >= getY()) return;

        AABB column = new AABB(getX(), groundY, getZ(), getX(), getY(), getZ()).inflate(5D, 0D, 5D);

        for (Entity entity : level.getEntities(this, column)) {

            if (!canTarget(entity)) continue;

            entity.hurtServer(
                    level, level.damageSources().source(ModDamageTypes.COMBINE, this), 1000F);
            entity.igniteForSeconds(5);

            if (entity instanceof LivingEntity living) {
                ContaminationUtil.contaminate(
                        living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            }
        }
    }

    private void tickWeapons() {

        boolean laserPhase = this.tickCount % 300 < 200;
        int period = laserPhase ? 4 : 20;

        if (this.tickCount % period == 0) {

            if (!this.secondaries.isEmpty()) {
                Entity victim = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
                if (!victim.isAlive() || victim.isSpectator()) this.secondaries.remove(victim);
                else shoot(victim, laserPhase);

            } else if (this.target != null) {
                shoot(this.target, laserPhase);
            }

        } else if (this.tickCount % period == period / 2 && this.target != null) {
            shoot(this.target, laserPhase);
        }
    }

    private void shoot(Entity victim, boolean laser) {
        if (laser) laserAttack(victim);
        else rocketAttack(victim);
    }

    private void laserAttack(Entity victim) {

        Vec3 vec =
                new Vec3(getX() - victim.getX(), 0D, getZ() - victim.getZ())
                        .yRot((float) Math.toRadians(-80 + this.random.nextInt(160)))
                        .normalize();

        double pivotX = getX() - vec.x * 10D;
        double pivotY = getY() + 0.5D;
        double pivotZ = getZ() - vec.z * 10D;

        Vec3 heading =
                new Vec3(
                                victim.getX() - pivotX,
                                victim.getY() + victim.getBbHeight() / 2D - pivotY,
                                victim.getZ() - pivotZ)
                        .normalize();

        BulletBaseMK4Entity bullet =
                new BulletBaseMK4Entity(
                        level(),
                        this,
                        XFactoryNPC.worm_laser,
                        XFactoryNPC.WORM_LASER_DAMAGE,
                        0.02F,
                        pivotX,
                        pivotY,
                        pivotZ,
                        heading.x,
                        heading.y,
                        heading.z);
        bullet.setAcceleration(1);
        level().addFreshEntity(bullet);

        WorldSounds.play(
                level(),
                null,
                getX(),
                getY(),
                getZ(),
                ModSounds.BALLS_LASER.get(),
                SoundSource.HOSTILE,
                5F,
                1F);
    }

    private void rocketAttack(Entity victim) {

        Vec3 heading =
                new Vec3(
                                victim.getX() - getX(),
                                victim.getY() + victim.getBbHeight() / 2D - getY() - 0.5D,
                                victim.getZ() - getZ())
                        .normalize();

        BulletBaseMK4Entity bullet =
                new BulletBaseMK4Entity(
                        level(),
                        this,
                        XFactoryNPC.ufo_rocket,
                        XFactoryNPC.UFO_ROCKET_DAMAGE,
                        0.02F,
                        getX(),
                        getY() - 0.5D,
                        getZ(),
                        heading.x,
                        heading.y,
                        heading.z);
        bullet.lockonTarget = victim;
        level().addFreshEntity(bullet);

        WorldSounds.play(
                level(),
                null,
                getX(),
                getY(),
                getZ(),
                ModSounds.RICHARD_FIRE.get(),
                SoundSource.HOSTILE,
                5F,
                1F);
    }

    private void approachWaypoint() {
        setDeltaMovement(Vec3.ZERO);
        if (this.courseChangeCooldown > 0
                && !steerToWaypoint(this.target instanceof Player ? 5D : 2D)) {
            this.courseChangeCooldown = 0;
        }
    }

    private int groundBelow() {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(getX());
        int z = Mth.floor(getZ());

        for (int y = (int) Math.ceil(getY()); y >= level().getMinY(); y--) {
            if (!level().getBlockState(pos.set(x, y, z)).isAir()) return y;
        }

        return level().getMinY();
    }

    private boolean canTarget(Entity entity) {
        return !(entity instanceof UFOEntity) && !(entity instanceof BulletBaseMK4Entity);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) bossEvent.setProgress(getHealth() / getMaxHealth());
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
    protected void tickDeath() {

        if (getBeam()) setBeam(false);

        setDeltaMovement(getDeltaMovement().subtract(0D, 0.05D, 0D));

        if (this.deathTime == -10) {
            WorldSounds.play(
                    level(),
                    null,
                    this,
                    ModSounds.CHOPPER_DAMAGE.get(),
                    SoundSource.HOSTILE,
                    10F,
                    1F);
        }

        if (this.deathTime == 19 && level() instanceof ServerLevel server) {
            server.explode(
                    this, getX(), getY(), getZ(), 10F, true, Level.ExplosionInteraction.BLOCK);
            MiniNukeExplosion.MEDIUM.detonate(server, getX(), getY(), getZ());

            HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.UFO, 200D);

            for (Player player :
                    server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200D))) {
                player.getInventory()
                        .placeItemBackInInventory(new ItemStack(ModItems.COIN_UFO.get()));
            }
        }

        super.tickDeath();
    }

    @Override
    protected float getSoundVolume() {
        return 10F;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM);
    }
}
