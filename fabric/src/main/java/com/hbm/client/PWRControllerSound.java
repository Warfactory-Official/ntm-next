// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachinePWRController;

public final class PWRControllerSound {

    private PWRControllerSound() {}

    public static void tick(BlockEntityMachinePWRController be) {
        be.audioLoop(be.amountLoaded > 0, 1F);
    }
}
