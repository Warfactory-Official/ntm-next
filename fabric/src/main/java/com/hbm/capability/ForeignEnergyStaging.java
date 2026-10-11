// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.energymk2.EnergyConversion;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ForeignEnergyStaging {

    private final BlockEntity be;
    private final IEnergyHandlerMK2 machine;
    private long insertFe;
    private long extractFe;

    public ForeignEnergyStaging(BlockEntity be, IEnergyHandlerMK2 machine) {
        this.be = be;
        this.machine = machine;
    }

    public long insertableFe(long maxFe, IEnergyHandlerMK2 receiver) {
        if (be.isRemoved() || maxFe <= 0) return 0;
        long offered =
                Math.min(receiver.getMaxPower() - receiver.getPower(), receiver.getReceiverSpeed());
        long quantum = EnergyConversion.feQuantum();
        return Math.max(
                0,
                Math.min(maxFe, EnergyConversion.feFromHe(offered) - insertFe) / quantum * quantum);
    }

    public long extractableFe(long maxFe, IEnergyHandlerMK2 provider) {
        if (be.isRemoved() || maxFe <= 0) return 0;
        long available = Math.min(provider.getPower(), provider.getProviderSpeed());
        long quantum = EnergyConversion.feQuantum();
        return Math.max(
                0,
                Math.min(maxFe, EnergyConversion.feFromHe(available) - extractFe)
                        / quantum
                        * quantum);
    }

    public void stageInsert(long fe) {
        insertFe += fe;
    }

    public void stageExtract(long fe) {
        extractFe += fe;
    }

    public long[] save() {
        return new long[] {insertFe, extractFe};
    }

    public void restore(long[] saved) {
        insertFe = saved[0];
        extractFe = saved[1];
    }

    public void commit() {
        if (insertFe > 0) machine.transferPower(EnergyConversion.heFromFe(insertFe), false);
        if (extractFe > 0) machine.usePower(EnergyConversion.heFromFe(extractFe));
        insertFe = 0;
        extractFe = 0;
        be.setChanged();
    }

    public long amountFe() {
        if (be.isRemoved()) return 0;
        return Math.max(0, EnergyConversion.feFromHe(machine.getPower()) + insertFe - extractFe);
    }

    public long capacityFe() {
        return be.isRemoved() ? 0 : EnergyConversion.feFromHe(machine.getMaxPower());
    }
}
