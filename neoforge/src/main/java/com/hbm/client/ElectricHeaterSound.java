// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityHeaterElectric;

public final class ElectricHeaterSound {

    private ElectricHeaterSound() {}

    public static void tick(BlockEntityHeaterElectric heater) {
        heater.audioLoop(heater.isOn, 1F);
    }
}
