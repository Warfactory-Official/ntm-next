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

package com.hbm.entity.grenade;

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.InterpolatedProjectileEntity;
import com.hbm.items.weapon.grenade.*;
import com.hbm.items.weapon.grenade.*;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GrenadeUniversalEntity extends InterpolatedProjectileEntity {

    public static final int TRAIL_TRIPLET = 1;

    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(
                    GrenadeUniversalEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> BOUNCES =
            SynchedEntityData.defineId(GrenadeUniversalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAIL =
            SynchedEntityData.defineId(GrenadeUniversalEntity.class, EntityDataSerializers.INT);

    public double prevSpin;
    public double spin;

    public GrenadeUniversalEntity(EntityType<? extends GrenadeUniversalEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeUniversalEntity(Level level, ItemStack grenade) {
        this(ModEntities.GRENADE_UNIVERSAL.get(), level);
        setGrenadeItem(grenade);
    }

    public GrenadeUniversalEntity(Level level, LivingEntity thrower, ItemStack grenade) {
        this(level, grenade);
        setThrower(thrower);
        double radians = Math.toRadians(-thrower.getYRot() + 180F);
        double offsetX = 0.25D * Math.cos(radians);
        double offsetZ = -0.25D * Math.sin(radians);
        setPos(
                thrower.getX() + offsetX,
                thrower.getY() + thrower.getEyeHeight() - 0.25D,
                thrower.getZ() + offsetZ);
        Vec3 yeet = thrower.getLookAngle().normalize();
        setThrowableHeading(yeet.x, yeet.y, yeet.z, (float) getShell().getYeetForce(), 0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GRENADE, ItemStack.EMPTY);
        builder.define(BOUNCES, 0);
        builder.define(TRAIL, 0);
    }

    public ItemStack getGrenadeItem() {
        return entityData.get(GRENADE);
    }

    private void setGrenadeItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        entityData.set(GRENADE, copy);
    }

    public int getBounces() {
        return entityData.get(BOUNCES);
    }

    public int getTrail() {
        return entityData.get(TRAIL);
    }

    public GrenadeUniversalEntity setTrail(int trail) {
        entityData.set(TRAIL, trail);
        return this;
    }

    public GrenadeShellItem.EnumGrenadeShell getShell() {
        return GrenadeUniversalItem.getData(getGrenadeItem()).shell();
    }

    public GrenadeFillingDefinition getFilling() {
        return GrenadeUniversalItem.getData(getGrenadeItem()).filling();
    }

    public GrenadeFuzeItem.EnumGrenadeFuze getFuze() {
        return GrenadeUniversalItem.getData(getGrenadeItem()).fuze();
    }

    public GrenadeExtraItem.EnumGrenadeExtra getExtra() {
        return GrenadeUniversalItem.getData(getGrenadeItem()).extra();
    }

    @Override
    public void tick() {
        var previous = flightStart();
        super.tick();
        if (isRemoved()) return;

        GrenadeFuzeItem.updateTick(this);
        if (isRemoved()) return;
        GrenadeExtraItem.updateTick(this);
        if (isRemoved()) return;

        if (level().isClientSide()) {
            prevSpin = spin;
            if (getBounces() <= 0) spin += 15D;
            else spin += Math.min(15D, position().subtract(previous).horizontalDistance() * 50D);
            if (spin >= 360D) {
                prevSpin -= 360D;
                spin -= 360D;
            }
            if (getTrail() == TRAIL_TRIPLET) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0D, 0D, 0D);
            }
        }
    }

    @Override
    public boolean doesImpactEntities() {
        return getFuze() == GrenadeFuzeItem.EnumGrenadeFuze.IMPACT && getTimer() >= 10;
    }

    @Override
    protected void onImpact(HitResult impact) {
        GrenadeFuzeItem.onImpact(this, impact);
        GrenadeExtraItem.onImpact(this, impact);

        if (isRemoved() || !(impact instanceof BlockHitResult blockHit)) return;
        Direction direction = blockHit.getDirection();
        Vec3 hit = blockHit.getLocation();
        setPos(
                hit.x + direction.getStepX() * 0.05D,
                hit.y + direction.getStepY() * 0.05D,
                hit.z + direction.getStepZ() * 0.05D);
        Vec3 velocity = getDeltaMovement();
        if (velocity.length() > 0.2D) {
            WorldSounds.play(
                    level(),
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.GRENADE_BOUNCE.get(),
                    SoundSource.BLOCKS,
                    1F,
                    1F);
        }
        double bounce = getShell().getBounce();
        setDeltaMovement(
                direction.getStepX() != 0 ? -velocity.x * bounce : velocity.x * 0.8D,
                direction.getStepY() != 0 ? -velocity.y * bounce : velocity.y * 0.8D,
                direction.getStepZ() != 0 ? -velocity.z * bounce : velocity.z * 0.8D);
        sendTeleport();
        entityData.set(BOUNCES, getBounces() + 1);
    }

    public void explode() {
        discard();
        GrenadeFillingItem.explode(this);
        GrenadeExtraItem.onExplode(this);
        if (!level().isClientSide() && ((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[GREN] Set off grenade at {} / {} / {} by {}!",
                    (int) getX(),
                    (int) getY(),
                    (int) getZ(),
                    getThrower() instanceof Player player
                            ? player.getDisplayName().getString()
                            : "null");
        }
    }

    public int getTimer() {
        return ticksInAir + ticksInGround;
    }

    @Override
    public boolean fullBlockCollisions() {
        return true;
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("grenade", ItemStack.OPTIONAL_CODEC, getGrenadeItem());
        output.putInt("bounces", getBounces());
        output.putInt("trail", getTrail());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("grenade", ItemStack.OPTIONAL_CODEC).ifPresent(this::setGrenadeItem);
        entityData.set(BOUNCES, input.getIntOr("bounces", 0));
        entityData.set(TRAIL, input.getIntOr("trail", 0));
    }
}
