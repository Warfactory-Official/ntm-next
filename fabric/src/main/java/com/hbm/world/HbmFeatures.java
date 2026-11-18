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

import com.hbm.registration.Registrar;
import com.hbm.world.feature.*;
import com.hbm.world.placement.LightBlockingHeightPlacement;
import com.hbm.world.placement.OreHostPlacement;
import com.hbm.world.placement.WorldConfigPlacement;

public final class HbmFeatures {
    private HbmFeatures() {}

    public static void register() {
        LightBlockingHeightPlacement.register();
        WorldConfigPlacement.register();
        OreHostPlacement.register();
        Registrar.registerFeature("stratum", StratumFeature.CODEC);
        Registrar.registerFeature("stratum_3d", Stratum3DFeature.CODEC);
        Registrar.registerFeature("depth_deposit", DepthDepositFeature.CODEC);
        Registrar.registerFeature("windowed_ore", WindowedOreFeature.CODEC);
        Registrar.registerFeature("coltan_deposit", ColtanDepositFeature.CODEC);
        Registrar.registerFeature("nether_surface", NetherSurfaceFeature.CODEC);
        Registrar.registerFeature("bedrock_ore", BedrockOreFeature.CODEC);
        Registrar.registerFeature("ore_cave", OreCaveFeature.CODEC);
        Registrar.registerFeature("oil_bubble", OilBubbleFeature.CODEC);
        Registrar.registerFeature("bedrock_oil_deposit", BedrockOilDepositFeature.CODEC);
        Registrar.registerFeature("meteorite", MeteoriteFeature.CODEC);
        Registrar.registerFeature("geyser", GeyserFeature.CODEC);
        Registrar.registerFeature("biome_cave", BiomeCaveFeature.CODEC);
        Registrar.registerFeature("glyphid_hive", GlyphidHiveFeature.CODEC);
        Registrar.registerFeature("dud", DudFeature.CODEC);
        Registrar.registerFeature("locked_safe", LockedSafeFeature.CODEC);
        Registrar.registerFeature("lantern_behemoth", LanternBehemothFeature.CODEC);
        Registrar.registerFeature("bosnia_mine", BosniaMineFeature.CODEC);
        Registrar.registerFeature("pink_log", PinkLogFeature.CODEC);
        Registrar.registerFeature("library_dungeon", LibraryDungeonFeature.CODEC);
        Registrar.registerFeature("barrel", BarrelFeature.CODEC);
        Registrar.registerFeature("arctic_vault", ArcticVaultFeature.CODEC);
        Registrar.registerFeature("flower_patch", FlowerPatchFeature.CODEC);
        Registrar.registerFeature("jungle_dungeon_marker", JungleDungeonMarkerFeature.CODEC);
        Registrar.registerFeature("capsule", CapsuleFeature.CODEC);
        Registrar.registerFeature("keyhole", KeyholeFeature.CODEC);
    }
}
