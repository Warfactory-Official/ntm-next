// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineDiesel;

public final class DieselSound {

    private DieselSound() {}

    public static void tick(BlockEntityMachineDiesel be) {
        be.audioLoop(be.wasOn, 1F);
    }
}
