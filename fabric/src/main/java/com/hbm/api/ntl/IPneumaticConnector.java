// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.ntl;

import net.minecraft.core.Direction;

public interface IPneumaticConnector {

    default boolean canConnectPneumatic(Direction dir) {
        return true;
    }
}
