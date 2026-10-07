// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.environment;

import java.util.Arrays;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;

public final class AtmosphereLayers {

    public interface Provider {
        boolean claims(Level level);

        void addLayers(EnvironmentAttributeSystem.Builder builder, Level level);
    }

    private static Provider[] providers = new Provider[0];

    private AtmosphereLayers() {}

    public static void register(Provider provider) {
        providers = Arrays.copyOf(providers, providers.length + 1);
        providers[providers.length - 1] = provider;
    }

    public static void addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        for (Provider provider : providers) {
            if (provider.claims(level)) provider.addLayers(builder, level);
        }
    }

    public static boolean claimed(Level level) {
        for (Provider provider : providers) {
            if (provider.claims(level)) return true;
        }
        return false;
    }
}
