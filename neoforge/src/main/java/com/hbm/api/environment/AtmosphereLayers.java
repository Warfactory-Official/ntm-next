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
