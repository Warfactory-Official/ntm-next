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

package com.hbm.entity.train;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.items.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class RailCarElectricEntity extends RailCarRidableEntity {
    private static final EntityDataAccessor<Integer> POWER =
            SynchedEntityData.defineId(RailCarElectricEntity.class, EntityDataSerializers.INT);

    public RailCarElectricEntity(EntityType<? extends RailCarElectricEntity> type, Level level) {
        super(type, level);
    }

    public abstract int getMaxPower();

    public abstract int getPowerConsumption();

    public boolean hasChargeSlot() {
        return false;
    }

    public int getChargeSlot() {
        return 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0);
    }

    @Override
    public boolean canAccelerate() {
        return true;
    }

    @Override
    public void consumeFuel() {}

    public void setPower(int power) {
        this.entityData.set(POWER, power);
    }

    public int getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide() && this.hasChargeSlot()) {

            ItemStack stack = this.getItem(this.getChargeSlot());

            if (stack.getItem() instanceof EnergyItem battery) {
                int powerNeeded = this.getMaxPower() - this.getPower();
                long powerProvided =
                        Math.min(battery.getDischargeRate(stack), battery.getCharge(stack));
                int powerTransfered = (int) Math.min(powerNeeded, powerProvided);

                if (powerTransfered > 0) {
                    battery.removeCharge(stack, powerTransfered);
                    this.setPower(this.getPower() + powerTransfered);
                }
            } else if (stack.is(ModItems.BATTERY_CREATIVE.get())) {
                this.setPower(this.getMaxPower());
            }
        }
    }
}
