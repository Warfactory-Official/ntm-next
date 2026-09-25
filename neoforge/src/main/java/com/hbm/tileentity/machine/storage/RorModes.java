// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.redstoneoverradio.IRORInteractive;

final class RorModes {

    private RorModes() {}

    static int select(int current, String[] params) {
        int mode = IRORInteractive.parseInt(params[0], 0, 3);
        if (mode != current) return mode;
        return params.length > 1 ? IRORInteractive.parseInt(params[1], 0, 3) : current;
    }
}
