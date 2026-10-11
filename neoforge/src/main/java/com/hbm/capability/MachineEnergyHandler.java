// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class MachineEnergyHandler implements EnergyHandler {

    private final Journal journal;
    private final @Nullable IEnergyHandlerMK2 receiver;
    private final @Nullable IEnergyHandlerMK2 provider;

    public MachineEnergyHandler(
            IEnergyHandlerMK2 machine,
            BlockEntity be,
            @Nullable IEnergyHandlerMK2 receiver,
            @Nullable IEnergyHandlerMK2 provider) {
        this.journal = Journal.of(be, machine);
        this.receiver = receiver;
        this.provider = provider;
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
        if (gives <= 0) return 0;
        journal.updateSnapshots(transaction);
        journal.staging.stageExtract(gives);
        return (int) gives;
    }

    private static final class Journal extends SnapshotJournal<long[]> {
        private final ForeignEnergyStaging staging;

        private Journal(BlockEntity be, IEnergyHandlerMK2 machine) {
            this.staging = new ForeignEnergyStaging(be, machine);
        }

        static Journal of(BlockEntity be, IEnergyHandlerMK2 machine) {
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
