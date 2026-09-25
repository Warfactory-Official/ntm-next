// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.platform.Services;
import com.hbm.world.HbmFeatureAttachments.Attachment;
import net.minecraft.tags.BiomeTags;

public final class HbmWorldgen {

    public static final BiomeTarget DESERT_OIL =
            BiomeTarget.vanilla("desert", "badlands", "eroded_badlands", "wooded_badlands");

    public static final BiomeTarget GEYSER_CHLORINE = BiomeTarget.vanilla("plains");

    public static final BiomeTarget CAPSULE_BEACH = BiomeTarget.vanilla("beach");

    public static final BiomeTarget FOXGLOVE_FORESTS =
            BiomeTarget.vanilla("forest", "flower_forest", "birch_forest", "dark_forest");

    public static final BiomeTarget NIGHTSHADE_FORESTS = BiomeTarget.vanilla("dark_forest");

    public static final BiomeTarget TOBACCO_JUNGLES = BiomeTarget.tags(BiomeTags.IS_JUNGLE);

    public static final BiomeTarget REEDS_RIVERS = BiomeTarget.vanilla("river", "frozen_river");

    public static final BiomeTarget REEDS_BEACHES = BiomeTarget.vanilla("beach", "snowy_beach");
    public static final BiomeTarget JUNGLE_DUNGEONS = BiomeTarget.tags(BiomeTags.IS_JUNGLE);

    private HbmWorldgen() {}

    @SuppressWarnings("unchecked")
    public static void register() {
        for (Attachment attachment : HbmFeatureAttachments.ALL) {
            Services.BIOME_MODIFIER.addFeaturesToBiome(
                    attachment.biomes(), attachment.step(), attachment.feature());
        }
    }
}
