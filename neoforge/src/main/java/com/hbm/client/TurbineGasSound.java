// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineTurbineGas;

public final class TurbineGasSound {

    private TurbineGasSound() {}

    public static void tick(BlockEntityMachineTurbineGas be) {
        be.audioLoop(be.rpm >= 10 && be.state != -1, 2F, 0.55F + 0.1F * be.rpm / 10F);
    }
}
