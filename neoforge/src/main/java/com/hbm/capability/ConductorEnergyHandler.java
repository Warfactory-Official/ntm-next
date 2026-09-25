// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.energymk2.ForeignEnergyBridge;
import com.hbm.api.energymk2.PowerNetwork;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class ConductorEnergyHandler implements EnergyHandler {

    private static final Map<ForeignEnergyBridge, WeakReference<Journal>> JOURNALS =
            new WeakHashMap<>();

    private final ServerLevel level;
    private final BlockPos pos;

    private final long peerKey;

    public ConductorEnergyHandler(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.peerKey = side == null ? this.pos.asLong() : this.pos.relative(side).asLong();

        PowerNetwork.bridgeAt(level, this.pos);
    }

    private static synchronized Journal journalFor(ForeignEnergyBridge bridge) {
        WeakReference<Journal> ref = JOURNALS.get(bridge);
        Journal journal = ref == null ? null : ref.get();
        if (journal == null)
            JOURNALS.put(bridge, new WeakReference<>(journal = new Journal(bridge)));
        return journal;
    }

    private @Nullable ForeignEnergyBridge bridge() {
        return PowerNetwork.bridgeAt(level, pos);
    }

    @Override
    public long getAmountAsLong() {
        ForeignEnergyBridge bridge = bridge();
        return bridge == null ? 0 : bridge.amountFe();
    }

    @Override
    public long getCapacityAsLong() {
        ForeignEnergyBridge bridge = bridge();
        return bridge == null ? 0 : bridge.capacityFe();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        ForeignEnergyBridge bridge = bridge();
        if (bridge == null || amount <= 0) return 0;
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
