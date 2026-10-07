// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public final class WornAccessories {
    private static Source[] sources = new Source[0];

    @FunctionalInterface
    public interface Source {
        void collect(Player player, List<Container> out);
    }

    private WornAccessories() {}

    public static void register(Source source) {
        sources = Arrays.copyOf(sources, sources.length + 1);
        sources[sources.length - 1] = source;
    }

    public static List<Container> of(Player player) {
        if (sources.length == 0) return List.of();
        List<Container> out = new ArrayList<>();
        for (Source source : sources) source.collect(player, out);
        return out;
    }
}
