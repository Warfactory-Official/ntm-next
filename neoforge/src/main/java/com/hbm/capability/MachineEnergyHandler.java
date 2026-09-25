// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.energymk2.EnergyConversion;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class MachineEnergyHandler implements EnergyHandler {

    private static final Map<BlockEntity, WeakReference<Core>> CORES = new WeakHashMap<>();

    private final Core core;
    private final IEnergyHandlerMK2 machine;
    private final @Nullable IEnergyHandlerMK2 receiver;
    private final @Nullable IEnergyHandlerMK2 provider;
    private final BlockEntity be;
    private final Direction side;

    public MachineEnergyHandler(IEnergyHandlerMK2 machine, BlockEntity be, Direction side) {
        this(
                machine,
                be,
                side,
                (declaredOf(be, machine) & MachineCaps.POWER_IN) != 0 ? machine : null,
                (declaredOf(be, machine) & MachineCaps.POWER_OUT) != 0 ? machine : null);
    }

    public MachineEnergyHandler(
            IEnergyHandlerMK2 machine,
            BlockEntity be,
            Direction side,
            @Nullable IEnergyHandlerMK2 receiver,
            @Nullable IEnergyHandlerMK2 provider) {
        this.core = coreFor(be, machine);
        this.machine = machine;
        this.receiver = receiver;
        this.provider = provider;
        this.be = be;
        this.side = side;
    }

    private static synchronized Core coreFor(BlockEntity be, IEnergyHandlerMK2 machine) {
        WeakReference<Core> ref = CORES.get(be);
        Core core = ref == null ? null : ref.get();
        if (core == null) {
            core = new Core(machine, be);
            CORES.put(be, new WeakReference<>(core));
        }
        return core;
    }

    @Override
    public long getAmountAsLong() {
        return be.isRemoved() ? 0 : EnergyConversion.feFromHe(machine.getPower());
    }

    @Override
    public long getCapacityAsLong() {
        return be.isRemoved() ? 0 : EnergyConversion.feFromHe(machine.getMaxPower());
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (be.isRemoved() || receiver == null) return 0;
        long room = receiver.getMaxPower() - receiver.getPower();
        long offered = Math.min(room, receiver.getReceiverSpeed());

        long acceptableFe = EnergyConversion.feFromHe(offered);
        long quantum = EnergyConversion.feQuantum();
        long fits = Math.min(amount, acceptableFe) / quantum * quantum;
        int accepted = (int) Math.max(0L, fits);
        if (accepted > 0) {
            core.updateSnapshots(transaction);
            receiver.transferPower(EnergyConversion.heFromFe(accepted), false);
        }
        return accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (be.isRemoved() || provider == null) return 0;
        long available = Math.min(provider.getPower(), provider.getProviderSpeed());
        long extractableFe = EnergyConversion.feFromHe(available);
        long quantum = EnergyConversion.feQuantum();
        long gives = Math.min(amount, extractableFe) / quantum * quantum;
        int extracted = (int) Math.max(0L, gives);
        if (extracted > 0) {
            core.updateSnapshots(transaction);
            provider.usePower(EnergyConversion.heFromFe(extracted));
        }
        return extracted;
    }

    private static final class Core extends SnapshotJournal<Long> {
        private final IEnergyHandlerMK2 machine;
        private final BlockEntity be;

        Core(IEnergyHandlerMK2 machine, BlockEntity be) {
            this.machine = machine;
            this.be = be;
        }

        @Override
        protected Long createSnapshot() {
            return machine.getPower();
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            machine.setPower(snapshot);
        }

        @Override
        protected void onRootCommit(Long originalState) {
            be.setChanged();
        }
    }

    private static int declaredOf(BlockEntity be, IEnergyHandlerMK2 machine) {
        return MachineCaps.declaredDomains(machine instanceof BlockEntity owner ? owner : be);
    }
}
