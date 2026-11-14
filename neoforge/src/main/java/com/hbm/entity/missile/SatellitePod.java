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

package com.hbm.entity.missile;

import com.hbm.blockentity.machine.SatDockBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.entity.projectile.InterpolatedProjectileEntity;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.StoredItems;
import com.hbm.packet.ClientEventConstants;
import com.hbm.util.InventoryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SatellitePod extends InterpolatedProjectileEntity implements StoredItems {

    private static final EntityDataAccessor<Integer> LEGS_DOWN =
            SynchedEntityData.defineId(SatellitePod.class, EntityDataSerializers.INT);
    private static final int UNLOAD_TICKS = 100;
    private static final double CEILING = 300D;

    public List<ItemStack> cargo = new ArrayList<>();
    public int dwell;
    public double speed = 0.75D;
    public int dockY;
    private float legs;
    private float legsO;

    public SatellitePod(EntityType<? extends SatellitePod> type, Level level) {
        super(type, level);
    }

    public SatellitePod deliver(int dockY, List<ItemStack> cargo) {
        this.dockY = dockY;
        var copies = new ArrayList<ItemStack>(cargo.size());
        for (var stack : cargo) copies.add(stack.copy());
        this.cargo = copies;
        UnstableFuses.changed(this);
        return this;
    }

    public boolean landing() {
        return !cargo.isEmpty();
    }

    public boolean legsDown() {
        return entityData.get(LEGS_DOWN) == 1;
    }

    public void setLegsDown(boolean down) {
        entityData.set(LEGS_DOWN, down ? 1 : 0);
    }

    public float legExtension(float partialTick) {
        return legsO + (legs - legsO) * partialTick;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEGS_DOWN, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            animate();
            return;
        }
        if (isRemoved()) return;
        if (landing()) {
            if (dwell > 0) {
                dwell++;
                setPos(getX(), Math.ceil(getY()), getZ());
                if (dwell >= UNLOAD_TICKS) unload();
            } else if (onGround()) {
                speed = 0D;
                dwell = 1;
                setLegsDown(true);
            } else {
                if (getY() < dockY + 17 && !legsDown()) setLegsDown(true);
                if (getY() < dockY + 25) speed -= 0.01D;
                speed = Mth.clamp(speed, 0.025D, 0.75D);
            }
            setDeltaMovement(0D, -speed, 0D);
        } else {
            setOnGround(false);
            speed += 0.01D;
            if (speed >= 0.2D) setLegsDown(false);
            speed = Mth.clamp(speed, 0D, 2D);
            setDeltaMovement(0D, speed, 0D);
            if (getY() > CEILING) discard();
        }
    }

    private void animate() {
        legsO = legs;
        legs = Mth.clamp(legs + (legsDown() ? 0.05F : -0.05F), 0F, 1F);
        double rise = getDeltaMovement().y;
        if (legs > 0F && rise < 0D || rise > 0D) {
            ClientEffects.spawnGasFlame(
                    level(),
                    getX(),
                    getY() + 0.5D,
                    getZ(),
                    0D,
                    -0.25D,
                    0D,
                    ClientEventConstants.GAS_FLAME_DEFAULT_SCALE);
        }
    }

    public void unload() {
        var level = (ServerLevel) level();
        var below = BlockPos.containing(getX(), getY() - 0.5D, getZ());
        if (level.getBlockState(below).is(ModBlocks.SAT_DOCK.get())
                && level.getBlockEntity(below) instanceof SatDockBlockEntity dock) {
            for (int i = 0; i < cargo.size(); i++) {
                var stack = cargo.get(i);
                if (stack.isEmpty()) continue;
                cargo.set(
                        i,
                        InventoryUtil.tryAddItemToInventory(
                                dock, 0, SatDockBlockEntity.CARGO_SLOTS - 1, stack));
            }
        }
        for (var stack : cargo) {
            if (!stack.isEmpty()) spawnAtLocation(level, stack, 0.25F);
        }
        cargo = new ArrayList<>();
        UnstableFuses.changed(this);
    }

    @Override
    protected void onImpact(HitResult result) {
        if (!(result instanceof BlockHitResult hit)) return;
        setPos(hit.getLocation());
        setOnGround(true);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public double getGravityVelocity() {
        return 0D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    public float getWaterDrag() {
        return 1F;
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < cargo.size(); i++) {
            var stack = cargo.get(i);
            if (visitor.visit(stack) && stack.isEmpty()) cargo.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("state", legsDown() ? 1 : 0);
        output.putInt("timer", dwell);
        output.putDouble("speed", speed);
        output.putInt("callerYPos", dockY);
        output.store("slots", ItemStack.OPTIONAL_CODEC.listOf(), cargo);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setLegsDown(input.getIntOr("state", 0) == 1);
        dwell = input.getIntOr("timer", 0);
        speed = input.getDoubleOr("speed", 0.75D);
        dockY = input.getIntOr("callerYPos", 0);
        cargo =
                new ArrayList<>(
                        input.read("slots", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
    }
}
