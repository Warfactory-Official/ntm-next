// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.explosion;

import java.util.Arrays;
import net.minecraft.server.level.ServerLevel;

public final class NukeDetonations {

    public interface Listener {
        void detonated(ServerLevel level, double x, double z, float radius);
    }

    private static Listener[] listeners = new Listener[0];

    private NukeDetonations() {}

    public static void register(Listener listener) {
        listeners = Arrays.copyOf(listeners, listeners.length + 1);
        listeners[listeners.length - 1] = listener;
    }

    public static void detonated(ServerLevel level, double x, double z, float radius) {
        for (Listener listener : listeners) listener.detonated(level, x, z, radius);
    }
}
