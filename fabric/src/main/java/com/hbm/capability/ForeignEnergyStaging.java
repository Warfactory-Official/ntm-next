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
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ForeignEnergyStaging {

    private final BlockEntity be;
    private final EnergyHandler machine;
    private long insertFe;
    private long extractFe;

    public ForeignEnergyStaging(BlockEntity be, EnergyHandler machine) {
        this.be = be;
        this.machine = machine;
    }

    public long insertableFe(long maxFe, EnergyHandler receiver) {
        if (be.isRemoved() || maxFe <= 0) return 0;
        long offered =
                Math.min(receiver.getMaxPower() - receiver.getPower(), receiver.getReceiveRate());
        long quantum = EnergyConversion.feQuantum();
        return Math.max(
                0,
                Math.min(maxFe, EnergyConversion.feFromHe(offered) - insertFe) / quantum * quantum);
    }

    public long extractableFe(long maxFe, EnergyHandler provider) {
        if (be.isRemoved() || maxFe <= 0) return 0;
        long available = Math.min(provider.getPower(), provider.getSendRate());
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
        if (insertFe > 0) machine.transferPower(EnergyConversion.heFromFe(insertFe));
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
