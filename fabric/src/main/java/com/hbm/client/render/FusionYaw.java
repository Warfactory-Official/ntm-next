// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.util.Facing;
import net.minecraft.core.Direction;

final class FusionYaw {

    private FusionYaw() {}

    static float of(Direction facing) {
        return Facing.yaw(facing, 90);
    }
}
