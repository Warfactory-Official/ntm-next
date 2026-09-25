// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.machine.BlockEntityChungus;

public final class ChungusSound {

    private ChungusSound() {}

    public static void tick(BlockEntityChungus be) {
        float speed = be.fanAcceleration / 25F;

        be.audioLoop(
                be.isTurning() || be.fanAcceleration > 0F, 0.5F * speed, 0.25F + 0.75F * speed);
    }
}
