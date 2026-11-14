/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.api.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Arrays;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import org.jspecify.annotations.Nullable;

public final class WeatherTints {
    public record Tint(RenderPipeline pipeline, OitPipelineSet oit, int rain, int snow) {}

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
