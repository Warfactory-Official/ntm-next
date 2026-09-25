// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum ConveyorLiftPart implements StringRepresentable {
    BOTTOM,
    MIDDLE,
    TOP;

    private final String id = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return id;
    }
}
