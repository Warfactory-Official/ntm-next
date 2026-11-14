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
        var destinationData =
                ((net.caffeinemc.mods.lithium.common.world.section.LithiumSectionData)
                                (destination))
                        .lithium$getSectionData();
        destinationData.setChangeListener(null);
        if (counts != null) destinationData.setCountsByFlag(counts.clone());
        if (randomTicks != null) destinationData.setRandomTickableBlocksByY(randomTicks.clone());
    }

    public static void replaced(ServerLevel level, long sectionPos, LevelChunkSection replacement) {
        if (!(level instanceof LithiumData data)) return;
        var callback = data.lithium$getData().chunkSectionChangeCallbacks().get(sectionPos);
        if (callback != null) {
            ((net.caffeinemc.mods.lithium.common.world.section.LithiumSectionData) (replacement))
                    .lithium$getSectionData()
                    .setChangeListener(callback);
            callback.onChunkSectionInvalidated(SectionPos.of(sectionPos));
        }
    }
}
