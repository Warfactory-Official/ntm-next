// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineTurbofan;

public final class TurbofanSound {

    private TurbofanSound() {}

    public static void tick(BlockEntityMachineTurbofan be) {
        be.audioLoop(
                be.momentum > 0,
                be.momentum / 50F,
                be.momentum / 200F + 0.5F + be.afterburner * 0.16F);
    }
}
