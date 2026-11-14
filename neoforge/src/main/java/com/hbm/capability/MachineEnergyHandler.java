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
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.MixedEnergyPlan;
import com.hbm.api.energymk2.MixedEnergyQuota;
import com.hbm.api.energymk2.PowerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class MachineEnergyHandler
        implements net.neoforged.neoforge.transfer.energy.EnergyHandler {

    private final Journal journal;
    private final @Nullable EnergyHandler receiver;
    private final @Nullable EnergyHandler provider;
    private final BlockEntity be;
    private final BlockPos face;
    private final Direction side;

    public MachineEnergyHandler(
            EnergyHandler machine,
            BlockEntity be,
            @Nullable EnergyHandler receiver,
            @Nullable EnergyHandler provider,
            BlockPos face,
            Direction side) {
        this.journal = Journal.of(be, machine);
        this.receiver = receiver;
        this.provider = provider;
        this.be = be;
        this.face = face.immutable();
        this.side = side;
    }

    private @Nullable MixedEnergyPlan plan() {
        return be.getLevel() instanceof ServerLevel level ? PowerNetwork.planAt(level, face) : null;
    }

    @Override
    public long getAmountAsLong() {
        return journal.staging.amountFe();
    }

    @Override
    public long getCapacityAsLong() {
        return journal.staging.capacityFe();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (receiver == null) return 0;
        long fits = journal.staging.insertableFe(amount, receiver);
        MixedEnergyPlan plan = fits <= 0 ? null : plan();
        int slot = plan == null ? -1 : plan.slot(face.asLong(), side, false);
        long[] reserved = {0};
        if (slot >= 0)
            fits =
                    MixedEnergyQuota.reserve(
                            plan, slot, fits, EnergyConversion.feQuantum(), transaction, reserved);
        if (fits <= 0) return 0;
        journal.updateSnapshots(transaction);
        journal.staging.stageInsert(fits);
        return (int) fits;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (provider == null) return 0;
        long gives = journal.staging.extractableFe(amount, provider);
        MixedEnergyPlan plan = gives <= 0 ? null : plan();
        int slot = plan == null ? -1 : plan.slot(face.asLong(), side, true);
        long[] reserved = {0};
        if (slot >= 0)
            gives =
                    MixedEnergyQuota.reserve(
                            plan, slot, gives, EnergyConversion.feQuantum(), transaction, reserved);
        if (gives <= 0) return 0;
        journal.updateSnapshots(transaction);
        journal.staging.stageExtract(gives);
        return (int) gives;
    }

    private static final class Journal extends SnapshotJournal<long[]> {
        private final ForeignEnergyStaging staging;

        private Journal(BlockEntity be, EnergyHandler machine) {
            this.staging = new ForeignEnergyStaging(be, machine);
        }

        static Journal of(BlockEntity be, EnergyHandler machine) {
            ForeignBridges bridges = ForeignBridges.of(be);
            Journal journal = bridges.energy(machine) instanceof Journal held ? held : null;
            if (journal == null) bridges.energy(machine, journal = new Journal(be, machine));
            return journal;
        }

        @Override
        protected long[] createSnapshot() {
            return staging.save();
        }

        @Override
        protected void revertToSnapshot(long[] snapshot) {
            staging.restore(snapshot);
        }

        @Override
        protected void onRootCommit(long[] originalState) {
            staging.commit();
        }
    }
}
