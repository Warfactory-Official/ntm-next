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

import com.hbm.api.energymk2.ForeignEnergyBridge;
import com.hbm.api.energymk2.MixedEnergyPlan;
import com.hbm.api.energymk2.MixedEnergyQuota;
import com.hbm.api.energymk2.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class ConductorEnergyHandler implements EnergyHandler {

    private final ServerLevel level;
    private final BlockPos pos;
    private final @Nullable Direction side;

    private final long peerKey;

    public ConductorEnergyHandler(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
        this.peerKey = side == null ? this.pos.asLong() : this.pos.relative(side).asLong();
        PowerNetwork.bridgeAt(level, this.pos);
    }

    private static Journal journalFor(ForeignEnergyBridge bridge) {
        Journal journal = bridge.loaderStaging instanceof Journal held ? held : null;
        if (journal == null) bridge.loaderStaging = journal = new Journal(bridge);
        return journal;
    }

    private @Nullable ForeignEnergyBridge bridge() {
        return PowerNetwork.bridgeAt(level, pos);
    }

    private int slot(@Nullable MixedEnergyPlan plan, boolean toForeign) {
        return plan == null || side == null ? -1 : plan.slot(pos.asLong(), side, toForeign);
    }

    @Override
    public long getAmountAsLong() {
        ForeignEnergyBridge bridge = bridge();
        if (bridge == null) return 0;
        MixedEnergyPlan plan = bridge.plan();
        int slot = slot(plan, true);
        return slot < 0
                ? bridge.amountFe()
                : Math.min(bridge.plannedExtractableFe(), plan.availableFe(slot));
    }

    @Override
    public long getCapacityAsLong() {
        ForeignEnergyBridge bridge = bridge();
        if (bridge == null) return 0;
        MixedEnergyPlan plan = bridge.plan();
        int slot = slot(plan, false);
        if (slot < 0) return bridge.capacityFe();
        long total =
                getAmountAsLong() + Math.min(bridge.plannedInsertableFe(), plan.availableFe(slot));
        return total < 0 ? Long.MAX_VALUE : total;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        ForeignEnergyBridge bridge = bridge();
        if (bridge == null || amount <= 0) return 0;
        MixedEnergyPlan plan = bridge.plan();
        int slot = slot(plan, false);
        if (slot >= 0) {
            long[] reserved = {0};
            long fe =
                    MixedEnergyQuota.reserve(
                            plan,
                            slot,
                            Math.min(amount, bridge.plannedInsertableFe()),
                            1,
                            transaction,
                            reserved);
            if (fe <= 0) return 0;
            journalFor(bridge).updateSnapshots(transaction);
            bridge.insertPlannedFe(fe);
            return (int) fe;
        }
        long offer = Math.min(amount, bridge.insertableFe());
        if (offer <= 0) return 0;
        journalFor(bridge).updateSnapshots(transaction);
        return (int) bridge.insertFe(offer, peerKey, level.getGameTime());
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        ForeignEnergyBridge bridge = bridge();
        if (bridge == null || amount <= 0) return 0;
        MixedEnergyPlan plan = bridge.plan();
        int slot = slot(plan, true);
        if (slot >= 0) {
            long[] reserved = {0};
            long fe =
                    MixedEnergyQuota.reserve(
                            plan,
                            slot,
                            Math.min(amount, bridge.plannedExtractableFe()),
                            1,
                            transaction,
                            reserved);
            if (fe <= 0) return 0;
            journalFor(bridge).updateSnapshots(transaction);
            long given = bridge.extractPlannedFe(fe);
            MixedEnergyQuota.settle(plan, slot, reserved[0], given);
            return (int) given;
        }
        long offer = Math.min(amount, bridge.extractableFe());
        if (offer <= 0) return 0;
        journalFor(bridge).updateSnapshots(transaction);
        return (int) bridge.extractFe(offer);
    }

    private static final class Journal extends SnapshotJournal<ForeignEnergyBridge.State> {

        private final ForeignEnergyBridge bridge;

        Journal(ForeignEnergyBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        protected ForeignEnergyBridge.State createSnapshot() {
            return bridge.snapshot();
        }

        @Override
        protected void revertToSnapshot(ForeignEnergyBridge.State snapshot) {
            bridge.restore(snapshot);
        }
    }
}
