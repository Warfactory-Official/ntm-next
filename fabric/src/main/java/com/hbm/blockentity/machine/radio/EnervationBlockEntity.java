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

package com.hbm.blockentity.machine.radio;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.mob.FBIDroneEntity;
import com.hbm.entity.mob.FBIEntity;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class EnervationBlockEntity extends MachineBlockEntity implements EnergyHandler {

    public static final long CAPACITY = 500_000L;
    public static final long PULSE_COST = 25_000L;

    public long storedEnergy;
    private boolean infinitePower;
    private AABB bounds;
    private DamageSource pulseDamage;

    public EnervationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOBOX.get(), pos, state, 0);
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        pulseDamage = server.damageSources().source(ModDamageTypes.ENERVATION);
        bounds =
                new AABB(
                        worldPosition.getX() - 15,
                        worldPosition.getY() - 15,
                        worldPosition.getZ() - 15,
                        worldPosition.getX() + 15,
                        worldPosition.getY() + 15,
                        worldPosition.getZ() + 15);
    }

    @Override
    public void tickServer() {
        if (!infinitePower) {
            if (storedEnergy < PULSE_COST) return;
            storedEnergy -= PULSE_COST;
            setChanged();
        }
        var server = (ServerLevel) level;
        for (var target :
                server.getEntitiesOfClass(
                        LivingEntity.class, bounds, EnervationBlockEntity::eligible)) {
            target.hurtServer(server, pulseDamage, 20F);
        }
    }

    private static boolean eligible(LivingEntity entity) {
        return entity instanceof Enemy
                && !(entity instanceof FBIEntity)
                && !(entity instanceof FBIDroneEntity);
    }

    public boolean hasSpark() {
        return infinitePower;
    }

    public void installSpark() {
        infinitePower = true;
        setChanged();
    }

    @Override
    public long getPower() {
        return storedEnergy;
    }

    @Override
    public void setPower(long stored) {
        storedEnergy = stored;
    }

    @Override
    public long getMaxPower() {
        return CAPACITY;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (infinitePower) {
            var spark =
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            new ItemStack(ModItems.BATTERY_SPARK.get()));
            level.addFreshEntity(spark);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void forgetBulkDrops() {
        if (infinitePower) {
            infinitePower = false;
            setChanged();
        }
        super.forgetBulkDrops();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedEnergy = input.getLongOr("power", 0L);
        infinitePower = input.getBooleanOr("infinite", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", storedEnergy);
        output.putBoolean("infinite", infinitePower);
    }
}
