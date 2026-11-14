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

package com.hbm.world;

import com.hbm.platform.BiomeInjection;
import com.hbm.world.HbmFeatureAttachments.Attachment;
import net.minecraft.resources.ResourceKey;
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
        Attachment pre = null;
        for (Attachment attachment : HbmFeatureAttachments.ALL) {
            if (!HbmFeatureAttachments.PRE_DECORATION.contains(attachment.feature())) {
                BiomeInjection.addFeaturesToBiome(
                        attachment.biomes(), attachment.step(), attachment.feature());
                continue;
            }
            assert pre == null
                    || pre.biomes().equals(attachment.biomes())
                            && pre.step() == attachment.step()
                            && attachment.switches().isEmpty();
            pre = attachment;
        }
        BiomeInjection.addFeaturesToBiome(
                pre.biomes(),
                pre.step(),
                HbmFeatureAttachments.PRE_DECORATION.toArray(ResourceKey[]::new));
    }
}
