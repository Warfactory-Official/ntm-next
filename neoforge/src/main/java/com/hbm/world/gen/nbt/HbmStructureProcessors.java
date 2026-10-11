// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;

public final class HbmStructureProcessors {

    public static RegistryHandle<MapCodec<PickOneProcessor>> PICK_ONE;
    public static RegistryHandle<MapCodec<BobbleRandomizerProcessor>> BOBBLE_RANDOMIZER;
    public static RegistryHandle<MapCodec<MultiblockAssemblyProcessor>> MULTIBLOCK_ASSEMBLY;
    public static RegistryHandle<MapCodec<MaybePlaceProcessor>> MAYBE_PLACE;
    public static RegistryHandle<MapCodec<LootPileProcessor>> LOOT_PILE;
    public static RegistryHandle<MapCodec<WeightedPlaceProcessor>> WEIGHTED_PLACE;
    public static RegistryHandle<MapCodec<LockPinProcessor>> LOCK_PINS;
    public static RegistryHandle<MapCodec<GravityProcessor>> GROUND_HEIGHT;
    public static RegistryHandle<MapCodec<ImpactVillageProcessor>> IMPACT_VILLAGE;
    public static RegistryHandle<MapCodec<SpawnerEntityProcessor>> SPAWNER_ENTITY;
    public static RegistryHandle<MapCodec<DuctFluidProcessor>> DUCT_FLUID;
    public static RegistryHandle<MapCodec<MarkerTableProcessor>> MARKER_TABLE;

    private HbmStructureProcessors() {}

    public static void register() {
        PICK_ONE =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "pick_one", PickOneProcessor.MAP_CODEC);
        BOBBLE_RANDOMIZER =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "bobble_randomizer", BobbleRandomizerProcessor.MAP_CODEC);
        MULTIBLOCK_ASSEMBLY =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "multiblock_assembly", MultiblockAssemblyProcessor.MAP_CODEC);
        MAYBE_PLACE =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "maybe_place", MaybePlaceProcessor.MAP_CODEC);
        LOOT_PILE =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "loot_pile", LootPileProcessor.MAP_CODEC);
        WEIGHTED_PLACE =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "weighted_place", WeightedPlaceProcessor.MAP_CODEC);
        LOCK_PINS =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "lock_pins", LockPinProcessor.MAP_CODEC);
        GROUND_HEIGHT =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "ground_height", GroundHeightProcessor.MAP_CODEC);
        IMPACT_VILLAGE =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "impact_village", ImpactVillageProcessor.MAP_CODEC);
        SPAWNER_ENTITY =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "spawner_entity", SpawnerEntityProcessor.MAP_CODEC);
        DUCT_FLUID =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "duct_fluid", DuctFluidProcessor.MAP_CODEC);
        MARKER_TABLE =
                NeoForgeRegistrar.registerStructureProcessorType(
                        "marker_table", MarkerTableProcessor.MAP_CODEC);
    }
}
