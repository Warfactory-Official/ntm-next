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

package com.hbm.platform;

import com.hbm.api.energymk2.EnergyProbe;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

public final class FabricTrEnergyBridge {

    private FabricTrEnergyBridge() {}

    public static @Nullable EnergyStorage find(
            Level level, BlockPos pos, @Nullable Direction side) {
        return FabricTrEnergy.AVAILABLE ? EnergyStorage.SIDED.find(level, pos, side) : null;
    }

    public static BlockLookupCache<EnergyStorage> createCache(
            ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return new Caps.ApiCache<>(
                level, pos, side, BlockApiCache.create(EnergyStorage.SIDED, level, pos));
    }

    public static @Nullable EnergyStorage findItem(Container container, int slot) {
        SingleSlotStorage<ItemVariant> backing = ContainerStorage.of(container, null).getSlot(slot);
        return ContainerItemContext.ofSingleSlot(backing).find(EnergyStorage.ITEM);
    }

    public static long insertable(EnergyStorage store) {
        try (Transaction tx = Transaction.openOuter()) {
            return store.insert(Integer.MAX_VALUE, tx);
        }
    }

    public static long extractable(EnergyStorage store) {
        try (Transaction tx = Transaction.openOuter()) {
            return store.extract(Integer.MAX_VALUE, tx);
        }
    }

    public static void probeEnergy(EnergyStorage store, EnergyProbe result) {
        long input;
        long output;
        int directions = 0;
        try (Transaction transaction = Transaction.openOuter()) {
            try (Transaction probe = transaction.openNested()) {
                input = store.insert(Integer.MAX_VALUE, probe);
                if (input > 0) directions |= EnergyProbe.RECEIVE;
                if (store.extract(Integer.MAX_VALUE, probe) > 0) directions |= EnergyProbe.SEND;
            }
            try (Transaction probe = transaction.openNested()) {
                output = store.extract(Integer.MAX_VALUE, probe);
                if (output > 0) directions |= EnergyProbe.SEND;
                if (store.insert(Integer.MAX_VALUE, probe) > 0) directions |= EnergyProbe.RECEIVE;
            }
        }
        result.energyProbe(input, output, directions);
    }

    public static long transfer(
            EnergyStorage store, long maxFe, long feQuantum, boolean simulate, boolean insert) {
        assert feQuantum > 0;
        long request = Math.min(maxFe, Integer.MAX_VALUE);
        request -= request % feQuantum;
        if (request <= 0) return 0;

        try (Transaction outer = Transaction.openOuter()) {
            long available;
            try (Transaction probe = outer.openNested()) {
                available = insert ? store.insert(request, probe) : store.extract(request, probe);
            }
            long exact = available - available % feQuantum;
            if (exact <= 0) return 0;

            long moved = insert ? store.insert(exact, outer) : store.extract(exact, outer);
            if (moved != exact) return 0;
            if (!simulate) outer.commit();
            return exact;
        }
    }
}
