// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.oil.BlockEntityMachinePyroOven;

public final class PyroOvenSound {

    private PyroOvenSound() {}

    public static void tick(BlockEntityMachinePyroOven be) {
        be.audioLoop(be.isProgressing, 1F);
    }
}
