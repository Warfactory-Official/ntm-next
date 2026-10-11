// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.blocks.ModBlocks;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.world.feature.*;
import com.hbm.world.placement.LightBlockingHeightPlacement;
import com.hbm.world.placement.OreHostPlacement;
import com.hbm.world.placement.WorldConfigPlacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class HbmFeatures {

    public static RegistryHandle<StratumFeature> STRATUM;
    public static RegistryHandle<Stratum3DFeature> STRATUM_3D;
    public static RegistryHandle<DepthDepositFeature> DEPTH_DEPOSIT;
    public static RegistryHandle<WindowedOreFeature> WINDOWED_ORE;
    public static RegistryHandle<ColtanDepositFeature> COLTAN_DEPOSIT;
    public static RegistryHandle<NetherSurfaceFeature> NETHER_SURFACE;
    public static RegistryHandle<BedrockOreFeature> BEDROCK_ORE;
    public static RegistryHandle<OreCaveFeature> ORE_CAVE;
    public static RegistryHandle<OilBubbleFeature> OIL_BUBBLE;
    public static RegistryHandle<BedrockOilDepositFeature> BEDROCK_OIL_DEPOSIT;

    public static RegistryHandle<MeteoriteFeature> METEORITE;
    public static RegistryHandle<GeyserFeature> GEYSER;
    public static RegistryHandle<BiomeCaveFeature> BIOME_CAVE;
    public static RegistryHandle<GlyphidHiveFeature> GLYPHID_HIVE;
    public static RegistryHandle<DudFeature> DUD;
    public static RegistryHandle<LockedSafeFeature> LOCKED_SAFE;
    public static RegistryHandle<LanternBehemothFeature> LANTERN_BEHEMOTH;
    public static RegistryHandle<BosniaMineFeature> BOSNIA_MINE;
    public static RegistryHandle<PinkLogFeature> PINK_LOG;
    public static RegistryHandle<LibraryDungeonFeature> LIBRARY_DUNGEON;
    public static RegistryHandle<BarrelFeature> BARREL;
    public static RegistryHandle<ArcticVaultFeature> ARCTIC_VAULT;
    public static RegistryHandle<FlowerPatchFeature> FOXGLOVE;
    public static RegistryHandle<FlowerPatchFeature> NIGHTSHADE;
    public static RegistryHandle<FlowerPatchFeature> TOBACCO;
    public static RegistryHandle<FlowerPatchFeature> WEED;
    public static RegistryHandle<FlowerPatchFeature> REEDS_RIVER;
    public static RegistryHandle<FlowerPatchFeature> REEDS_BEACH;
    public static RegistryHandle<JungleDungeonMarkerFeature> JUNGLE_DUNGEON_MARKER;
    public static RegistryHandle<CapsuleFeature> CAPSULE;
    public static RegistryHandle<KeyholeFeature> KEYHOLE;

    private HbmFeatures() {}

    public static void register() {
        LightBlockingHeightPlacement.register();
        WorldConfigPlacement.register();
        OreHostPlacement.register();
        STRATUM = NeoForgeRegistrar.registerFeature("stratum", new StratumFeature());
        STRATUM_3D = NeoForgeRegistrar.registerFeature("stratum_3d", new Stratum3DFeature());
        DEPTH_DEPOSIT =
                NeoForgeRegistrar.registerFeature("depth_deposit", new DepthDepositFeature());
        WINDOWED_ORE = NeoForgeRegistrar.registerFeature("windowed_ore", new WindowedOreFeature());
        COLTAN_DEPOSIT =
                NeoForgeRegistrar.registerFeature("coltan_deposit", new ColtanDepositFeature());
        NETHER_SURFACE =
                NeoForgeRegistrar.registerFeature("nether_surface", new NetherSurfaceFeature());
        BEDROCK_ORE = NeoForgeRegistrar.registerFeature("bedrock_ore", new BedrockOreFeature());
        ORE_CAVE = NeoForgeRegistrar.registerFeature("ore_cave", new OreCaveFeature());
        OIL_BUBBLE = NeoForgeRegistrar.registerFeature("oil_bubble", new OilBubbleFeature());
        BEDROCK_OIL_DEPOSIT =
                NeoForgeRegistrar.registerFeature(
                        "bedrock_oil_deposit", new BedrockOilDepositFeature());
        METEORITE = NeoForgeRegistrar.registerFeature("meteorite", new MeteoriteFeature());
        GEYSER = NeoForgeRegistrar.registerFeature("geyser", new GeyserFeature());
        BIOME_CAVE = NeoForgeRegistrar.registerFeature("biome_cave", new BiomeCaveFeature());
        GLYPHID_HIVE = NeoForgeRegistrar.registerFeature("glyphid_hive", new GlyphidHiveFeature());
        DUD = NeoForgeRegistrar.registerFeature("dud", new DudFeature());
        LOCKED_SAFE = NeoForgeRegistrar.registerFeature("locked_safe", new LockedSafeFeature());
        LANTERN_BEHEMOTH =
                NeoForgeRegistrar.registerFeature("lantern_behemoth", new LanternBehemothFeature());
        BOSNIA_MINE = NeoForgeRegistrar.registerFeature("bosnia_mine", new BosniaMineFeature());
        PINK_LOG = NeoForgeRegistrar.registerFeature("pink_log", new PinkLogFeature());
        LIBRARY_DUNGEON =
                NeoForgeRegistrar.registerFeature("library_dungeon", new LibraryDungeonFeature());
        BARREL = NeoForgeRegistrar.registerFeature("barrel", new BarrelFeature());
        ARCTIC_VAULT = NeoForgeRegistrar.registerFeature("arctic_vault", new ArcticVaultFeature());
        FOXGLOVE =
                NeoForgeRegistrar.registerFeature(
                        "foxglove",
                        new FlowerPatchFeature(() -> ModBlocks.PLANT_FLOWER_FOXGLOVE.get(), 16));
        NIGHTSHADE =
                NeoForgeRegistrar.registerFeature(
                        "nightshade",
                        new FlowerPatchFeature(() -> ModBlocks.PLANT_FLOWER_NIGHTSHADE.get(), 8));
        TOBACCO =
                NeoForgeRegistrar.registerFeature(
                        "tobacco",
                        new FlowerPatchFeature(() -> ModBlocks.PLANT_FLOWER_TOBACCO.get(), 8));
        WEED =
                NeoForgeRegistrar.registerFeature(
                        "weed",
                        new FlowerPatchFeature(() -> ModBlocks.PLANT_FLOWER_WEED.get(), 64));
        REEDS_RIVER =
                NeoForgeRegistrar.registerFeature(
                        "reeds_river",
                        new FlowerPatchFeature(() -> ModBlocks.PLANT_REEDS.get(), 4));
        REEDS_BEACH =
                NeoForgeRegistrar.registerFeature(
                        "reeds_beach",
                        new FlowerPatchFeature(() -> ModBlocks.PLANT_REEDS.get(), 8));
        JUNGLE_DUNGEON_MARKER =
                NeoForgeRegistrar.registerFeature(
                        "jungle_dungeon_marker", new JungleDungeonMarkerFeature());
        CAPSULE = NeoForgeRegistrar.registerFeature("capsule", new CapsuleFeature());
        KEYHOLE = NeoForgeRegistrar.registerFeature("keyhole", new KeyholeFeature());
    }

    public static Feature<StratumConfig> stratum() {
        return STRATUM.get();
    }

    public static Feature<Stratum3DConfig> stratum3d() {
        return STRATUM_3D.get();
    }

    public static Feature<DepthDepositConfig> depthDeposit() {
        return DEPTH_DEPOSIT.get();
    }

    public static Feature<WindowedOreConfig> windowedOre() {
        return WINDOWED_ORE.get();
    }

    public static Feature<ColtanDepositConfig> coltanDeposit() {
        return COLTAN_DEPOSIT.get();
    }

    public static Feature<NetherSurfaceConfig> netherSurface() {
        return NETHER_SURFACE.get();
    }

    public static Feature<BedrockOreConfig> bedrockOre() {
        return BEDROCK_ORE.get();
    }

    public static Feature<OreCaveConfig> oreCave() {
        return ORE_CAVE.get();
    }

    public static Feature<OilBubbleConfig> oilBubble() {
        return OIL_BUBBLE.get();
    }

    public static Feature<BedrockOilDepositConfig> bedrockOilDeposit() {
        return BEDROCK_OIL_DEPOSIT.get();
    }

    public static Feature<NoneFeatureConfiguration> meteorite() {
        return METEORITE.get();
    }

    public static Feature<NoneFeatureConfiguration> geyser() {
        return GEYSER.get();
    }

    public static Feature<NoneFeatureConfiguration> capsule() {
        return CAPSULE.get();
    }

    public static Feature<NoneFeatureConfiguration> biomeCave() {
        return BIOME_CAVE.get();
    }

    public static Feature<NoneFeatureConfiguration> glyphidHive() {
        return GLYPHID_HIVE.get();
    }

    public static Feature<NoneFeatureConfiguration> dud() {
        return DUD.get();
    }

    public static Feature<NoneFeatureConfiguration> lanternBehemoth() {
        return LANTERN_BEHEMOTH.get();
    }

    public static Feature<NoneFeatureConfiguration> libraryDungeon() {
        return LIBRARY_DUNGEON.get();
    }

    public static Feature<NoneFeatureConfiguration> arcticVault() {
        return ARCTIC_VAULT.get();
    }

    public static Feature<NoneFeatureConfiguration> barrel() {
        return BARREL.get();
    }

    public static Feature<NoneFeatureConfiguration> foxglove() {
        return FOXGLOVE.get();
    }

    public static Feature<NoneFeatureConfiguration> nightshade() {
        return NIGHTSHADE.get();
    }

    public static Feature<NoneFeatureConfiguration> tobacco() {
        return TOBACCO.get();
    }

    public static Feature<NoneFeatureConfiguration> weed() {
        return WEED.get();
    }

    public static Feature<NoneFeatureConfiguration> reedsRiver() {
        return REEDS_RIVER.get();
    }

    public static Feature<NoneFeatureConfiguration> reedsBeach() {
        return REEDS_BEACH.get();
    }
}
