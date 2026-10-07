// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockBobble.Bobble;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BobbleFigures {

    private static final double[] ZERO = {0, 0, 0};
    public static final RenderBobble.Pose NO_POSE =
            new RenderBobble.Pose(ZERO, ZERO, ZERO, ZERO, 0, ZERO);
    private static final Map<Bobble, RenderBobble.Figure> FIGURES = new ConcurrentHashMap<>();

    private BobbleFigures() {}

    public static void register(Bobble type, RenderBobble.Figure figure) {
        if (type instanceof BobbleType || FIGURES.putIfAbsent(type, figure) != null) {
            throw new IllegalStateException("bobblehead " + type.id() + " already has a figure");
        }
    }

    public static RenderBobble.Figure get(Bobble type) {
        RenderBobble.Figure figure = FIGURES.get(type);
        if (figure == null)
            throw new IllegalStateException("bobblehead " + type.id() + " has no figure");
        return figure;
    }
}
