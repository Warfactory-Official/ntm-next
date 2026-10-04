// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface FacedContext {
    @Nullable Direction side();

    static @Nullable Direction sideOf(@Nullable Object context) {
        if (context instanceof Direction side) return side;
        return context instanceof FacedContext faced ? faced.side() : null;
    }
}
