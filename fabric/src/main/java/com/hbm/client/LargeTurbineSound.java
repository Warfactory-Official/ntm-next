// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityMachineLargeTurbine;

public final class LargeTurbineSound {

    private LargeTurbineSound() {}

    public static void tick(BlockEntityMachineLargeTurbine be) {
        float speed = be.fanAcceleration / 15F;

        be.audioLoop(
                be.operational || be.fanAcceleration > 0F, 0.4F * speed, 0.25F + 0.75F * speed);
    }
}
