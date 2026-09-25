// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntitySoyuzLauncher;

public final class SoyuzLauncherSound {

    private SoyuzLauncherSound() {}

    public static void tick(BlockEntitySoyuzLauncher be) {
        be.audioLoop(be.starting && be.countdown > 0, 100F);
    }
}
