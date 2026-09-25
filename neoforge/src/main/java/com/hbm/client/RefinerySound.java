// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.oil.BlockEntityMachineRefinery;

public final class RefinerySound {

    private RefinerySound() {}

    public static void tick(BlockEntityMachineRefinery be) {
        if (be.isOn) be.audioTime = 20;

        boolean running = be.audioTime > 0;
        if (running) be.audioTime--;
        be.audioLoop(running, 1F);
    }
}
