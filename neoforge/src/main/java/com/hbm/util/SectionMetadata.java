// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.integration.lithium.LithiumSectionCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.fml.ModList;

public final class SectionMetadata {
    private static final boolean LITHIUM = ModList.get().isLoaded("lithium");

    private SectionMetadata() {}

    public static void copy(LevelChunkSection source, LevelChunkSection destination) {
        if (LITHIUM) LithiumSectionCompat.copy(source, destination);
    }

    public static void replaced(ServerLevel level, long sectionPos, LevelChunkSection replacement) {
        if (LITHIUM) LithiumSectionCompat.replaced(level, sectionPos, replacement);
    }
}
