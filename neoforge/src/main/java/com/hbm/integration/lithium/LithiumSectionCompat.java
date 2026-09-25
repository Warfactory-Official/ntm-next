// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.lithium;

import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.caffeinemc.mods.lithium.common.world.section.LithiumSectionData;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class LithiumSectionCompat {
    private LithiumSectionCompat() {}

    public static void copy(LevelChunkSection source, LevelChunkSection destination) {
        if (!(source instanceof LithiumSectionData data)) return;
        var sourceData = data.lithium$getSectionData();
        short[] counts = sourceData.getCountsByFlag();
        byte[] randomTicks = sourceData.getRandomTickableBlocksByY();
        if (counts == null && randomTicks == null) return;
        var destinationData = ((LithiumSectionData) destination).lithium$getSectionData();
        destinationData.setChangeListener(null);
        if (counts != null) destinationData.setCountsByFlag(counts.clone());
        if (randomTicks != null) destinationData.setRandomTickableBlocksByY(randomTicks.clone());
    }

    public static void replaced(ServerLevel level, long sectionPos, LevelChunkSection replacement) {
        if (!(level instanceof LithiumData data)) return;
        var callback = data.lithium$getData().chunkSectionChangeCallbacks().get(sectionPos);
        if (callback != null) {

            ((LithiumSectionData) replacement).lithium$getSectionData().setChangeListener(callback);
            callback.onChunkSectionInvalidated(SectionPos.of(sectionPos));
        }
    }
}
