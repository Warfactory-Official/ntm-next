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

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.explosion.assembly.ChargeYield;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FallingNukeEntity extends Projectile {

    private static final EntityDataAccessor<Byte> NUKE_META =
            SynchedEntityData.defineId(FallingNukeEntity.class, EntityDataSerializers.BYTE);
    private static final double AIR_DRAG = 0.99D;
    private static final double GRAVITY = 0.05D;
    private static final double TERMINAL_VELOCITY = -1.0D;
    private static final float NOSE_DIP = 2F;
    private static final float STEEPEST_PITCH = -75F;
    private static final float RELEASE_ANGLE = 90F;
    private static final ChargeYield INERT = new ChargeYield(0F, 0F, 0F, 0F, 0F, 0F, 0F);

    private ChargeYield yields = INERT;

    public FallingNukeEntity(EntityType<? extends FallingNukeEntity> type, Level level) {
        super(type, level);
    }

    public FallingNukeEntity(
            Level level,
            float tnt,
            float nuke,
            float hydro,
            float amat,
            float dirty,
            float schrab,
            float euph) {
        this(ModEntities.FALLING_BOMB.get(), level);
        yields = new ChargeYield(tnt, nuke, hydro, amat, dirty, schrab, euph);
        setYRot(RELEASE_ANGLE);
        yRotO = RELEASE_ANGLE;
        setXRot(RELEASE_ANGLE);
        xRotO = RELEASE_ANGLE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(NUKE_META, (byte) 0);
    }

    public int getNukeMeta() {
        return entityData.get(NUKE_META);
    }

    public void setNukeMeta(int meta) {
        entityData.set(NUKE_META, (byte) meta);
    }

    @Override
    public void tick() {
        super.tick();
        fall();
        if (level().isClientSide() || level().getBlockState(blockPosition()).isAir()) return;
        var charge = yields;
        ExplosionNukeCustom.explodeCustom(
                level(),
                getX(),
                getY(),
                getZ(),
                charge.conventional(),
                charge.nuclear(),
                charge.thermonuclear(),
                charge.antimatter(),
                charge.fallout(),
                charge.schrabidium(),
                charge.antiMass());
        discard();
    }

    private void fall() {
        var velocity = getDeltaMovement();
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        setDeltaMovement(
                velocity.x * AIR_DRAG,
                Math.max(velocity.y - GRAVITY, TERMINAL_VELOCITY),
                velocity.z * AIR_DRAG);
        if (getXRot() > STEEPEST_PITCH) setXRot(getXRot() - NOSE_DIP);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        yields =
                new ChargeYield(
                        input.getFloatOr("tnt", 0F),
                        input.getFloatOr("nuke", 0F),
                        input.getFloatOr("hydro", 0F),
                        input.getFloatOr("amat", 0F),
                        input.getFloatOr("dirty", 0F),
                        input.getFloatOr("schrab", 0F),
                        input.getFloatOr("euph", 0F));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("tnt", yields.conventional());
        output.putFloat("nuke", yields.nuclear());
        output.putFloat("hydro", yields.thermonuclear());
        output.putFloat("amat", yields.antimatter());
        output.putFloat("dirty", yields.fallout());
        output.putFloat("schrab", yields.schrabidium());
        output.putFloat("euph", yields.antiMass());
    }
}
