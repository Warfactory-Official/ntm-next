// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.biome;

import com.hbm.lib.Library;
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
        return ResourceKey.create(Registries.BIOME, Library.id(path));
    }
}
