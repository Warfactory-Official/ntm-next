// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.client;

import java.util.Arrays;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;

public final class ParticleSpawns {

    public interface Listener {
        void created(ParticleOptions options, double x, double y, double z, Particle particle);
    }

    private static Listener[] listeners = new Listener[0];

    private ParticleSpawns() {}

    public static void register(Listener listener) {
        listeners = Arrays.copyOf(listeners, listeners.length + 1);
        listeners[listeners.length - 1] = listener;
    }

    public static void created(
            ParticleOptions options, double x, double y, double z, Particle particle) {
        for (Listener listener : listeners) listener.created(options, x, y, z, particle);
    }
}
