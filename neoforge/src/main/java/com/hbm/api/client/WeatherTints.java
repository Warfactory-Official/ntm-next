// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Arrays;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;

public final class WeatherTints {
    public record Tint(
            RenderPipeline depthWrite, RenderPipeline noDepthWrite, int rain, int snow) {}

    public interface Provider {
        @Nullable Tint tint(ClientLevel level);
    }

    private static Provider[] providers = new Provider[0];

    private WeatherTints() {}

    public static void register(Provider provider) {
        providers = Arrays.copyOf(providers, providers.length + 1);
        providers[providers.length - 1] = provider;
    }

    public static @Nullable Tint tint(ClientLevel level) {
        for (Provider provider : providers) {
            Tint tint = provider.tint(level);
            if (tint != null) return tint;
        }
        return null;
    }
}
