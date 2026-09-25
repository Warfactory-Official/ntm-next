// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;

public final class CombustionEngineSound {

    private CombustionEngineSound() {}

    public static void tick(BlockEntityMachineCombustionEngine be) {
        be.audioLoop(be.wasOn, 1F);
    }
}
