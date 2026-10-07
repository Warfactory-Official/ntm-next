// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockPlushie.Plushie;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlushieFigures {

    private static final Map<Plushie, RenderPlushie.Figure> FIGURES = new ConcurrentHashMap<>();

    private PlushieFigures() {}

    public static void register(Plushie type, RenderPlushie.Figure figure) {
        if (type instanceof PlushieType || FIGURES.putIfAbsent(type, figure) != null) {
            throw new IllegalStateException("plushie " + type.id() + " already has a figure");
        }
    }

    static RenderPlushie.Figure get(Plushie type) {
        RenderPlushie.Figure figure = FIGURES.get(type);
        if (figure == null)
            throw new IllegalStateException("plushie " + type.id() + " has no figure");
        return figure;
    }

    static Collection<RenderPlushie.Figure> all() {
        return FIGURES.values();
    }
}
