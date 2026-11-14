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

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import com.hbm.extprop.LivingHazards;
import com.hbm.particle.helper.FlameCreator;
import java.util.function.Consumer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FireLingeringEntity extends Entity {

    public static final int TYPE_DIESEL = 0;
    public static final int TYPE_BALEFIRE = 1;
    public static final int TYPE_PHOSPHORUS = 2;
    public static final int TYPE_OXY = 3;
    public static final int TYPE_BLACK = 4;
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(FireLingeringEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(FireLingeringEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(FireLingeringEntity.class, EntityDataSerializers.FLOAT);
    private static final float WIDE = 5F;
    private static final float OBJECT_BURN_SECONDS = 4F;

    public int maxAge = 150;

    public FireLingeringEntity(EntityType<? extends FireLingeringEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public FireLingeringEntity(Level level) {
        this(ModEntities.FIRE_LINGERING.get(), level);
    }

    public FireLingeringEntity setArea(float width, float height) {
        entityData.set(WIDTH, width);
        entityData.set(HEIGHT, height);
        return this;
    }

    public FireLingeringEntity setDuration(int duration) {
        maxAge = duration;
        return this;
    }

    public FireLingeringEntity setType(int type) {
        entityData.set(TYPE, type);
        return this;
    }

    public int getFireType() {
        return entityData.get(TYPE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
        builder.define(WIDTH, 0F);
        builder.define(HEIGHT, 0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (WIDTH.equals(accessor) || HEIGHT.equals(accessor)) refreshDimensions();
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(entityData.get(WIDTH), entityData.get(HEIGHT));
    }

    @Override
    public void tick() {
        var fuel = Fuel.BY_TYPE[getFireType()];
        if (level().isClientSide()) flicker(fuel);
        else scorch(fuel);
    }

    private void scorch(Fuel fuel) {
        if (tickCount >= maxAge) discard();
        for (var target : level().getEntities(this, getBoundingBox())) {
            if (target instanceof LivingEntity living)
                fuel.burn.accept(LivingHazards.getOrCreate(living));
            else target.igniteForSeconds(OBJECT_BURN_SECONDS);
        }
    }

    private void flicker(Fuel fuel) {
        if (fuel.flame < 0) return;
        var width = getBbWidth();
        var floor = getY() - getBbHeight();
        var top = getBoundingBox().maxY;
        var minX = getBoundingBox().minX;
        var minZ = getBoundingBox().minZ;
        for (var i = width >= WIDE ? 2 : 1; i > 0; i--) {
            var x = minX + random.nextDouble() * width;
            var z = minZ + random.nextDouble() * width;
            var hit =
                    level().clip(
                                    new ClipContext(
                                            new Vec3(x, top, z),
                                            new Vec3(x, floor, z),
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            this));
            var y = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y : floor;
            FlameCreator.composeEffectClient(level(), x, y, z, fuel.flame);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    private enum Fuel {
        DIESEL(FlameCreator.META_FIRE, props -> props.fire = Math.max(props.fire, 60)),
        BALEFIRE(
                FlameCreator.META_BALEFIRE,
                props -> props.balefire = Math.max(props.balefire, 100)),
        PHOSPHORUS(FlameCreator.META_FIRE, props -> props.fire = Math.max(props.fire, 300)),
        OXY(-1, _ -> {}),
        BLACK(
                FlameCreator.META_BLACK,
                props -> props.blackFire = props.blackFire < 200 ? 200 : props.blackFire + 5);

        private static final Fuel[] BY_TYPE = values();

        private final int flame;
        private final Consumer<LivingHazards> burn;

        Fuel(int flame, Consumer<LivingHazards> burn) {
            this.flame = flame;
            this.burn = burn;
        }
    }
}
