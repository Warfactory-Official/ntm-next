// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.integration.lithium.LithiumSectionCompat;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class SectionMetadata {
    private static final boolean LITHIUM = Services.PLATFORM.isModLoaded("lithium");

    private SectionMetadata() {}

    public static void copy(LevelChunkSection source, LevelChunkSection destination) {
        if (LITHIUM) LithiumSectionCompat.copy(source, destination);
    }

    public static void replaced(ServerLevel level, long sectionPos, LevelChunkSection replacement) {
        if (LITHIUM) LithiumSectionCompat.replaced(level, sectionPos, replacement);
    }
}
