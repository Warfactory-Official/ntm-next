// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.lib.Library;
import com.hbm.registration.OverridePoint;
import net.minecraft.world.entity.Entity;

public final class RiderTilt {
    public static final float PIVOT = 1.62F;

    public static final OverridePoint<Source> POINT =
            OverridePoint.of(Library.id("rider_tilt"), (vehicle, partialTick) -> 0F);

    private RiderTilt() {}

    @FunctionalInterface
    public interface Source {
        float degrees(Entity vehicle, float partialTick);
    }
}
