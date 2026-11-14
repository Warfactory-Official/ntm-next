/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.capability;

import com.hbm.api.energymk2.EnergyConversion;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class BatteryEnergyHandler implements EnergyHandler {

    private final ItemAccess access;

    public BatteryEnergyHandler(ItemAccess access) {
        this.access = access;
    }

    private ItemStack stack() {
        return access.getResource().toStack();
    }

    private boolean exchange(ItemStack updated, TransactionContext transaction) {
        return access.exchange(ItemResource.of(updated), 1, transaction) == 1;
    }

    @Override
    public long getAmountAsLong() {
        return EnergyConversion.feFromHe(ForeignEnergyItemAccess.chargeHe(stack()));
    }

    @Override
    public long getCapacityAsLong() {
        return EnergyConversion.feFromHe(ForeignEnergyItemAccess.capacityHe(stack()));
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        ItemStack held = stack();
        long quantum = EnergyConversion.feQuantum();
        long accepted =
                ForeignEnergyItemAccess.insertableHe(held, EnergyConversion.heFromFe(amount));
        long fe = EnergyConversion.feFromHe(accepted) / quantum * quantum;
        if (fe <= 0) return 0;
        long he = EnergyConversion.heFromFe(fe);
        return exchange(ForeignEnergyItemAccess.withCharge(held, he), transaction) ? (int) fe : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        ItemStack held = stack();
        long quantum = EnergyConversion.feQuantum();
        long given = ForeignEnergyItemAccess.extractableHe(held, EnergyConversion.heFromFe(amount));
        long fe = EnergyConversion.feFromHe(given) / quantum * quantum;
        if (fe <= 0) return 0;
        if (ForeignEnergyItemAccess.isCreative(held)) return (int) fe;
        long he = EnergyConversion.heFromFe(fe);
        return exchange(ForeignEnergyItemAccess.withCharge(held, -he), transaction) ? (int) fe : 0;
    }
}
