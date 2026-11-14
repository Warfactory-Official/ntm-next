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

package com.hbm.world.biome;

import com.hbm.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class NtmBiomes {

    public static final ResourceKey<Biome> CRATER = key("crater");
    public static final ResourceKey<Biome> CRATER_INNER = key("crater_inner");
    public static final ResourceKey<Biome> CRATER_OUTER = key("crater_outer");

    private NtmBiomes() {}

    public static boolean isCrater(Holder<Biome> biome) {
        return biome.is(CRATER) || biome.is(CRATER_INNER) || biome.is(CRATER_OUTER);
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, RegistryUtil.id(path));
    }
}
