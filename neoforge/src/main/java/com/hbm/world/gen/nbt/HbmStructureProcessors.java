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

package com.hbm.world.gen.nbt;

import com.hbm.registration.Registrar;
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
        PICK_ONE = Registrar.registerStructureProcessorType("pick_one", PickOneProcessor.MAP_CODEC);
        BOBBLE_RANDOMIZER =
                Registrar.registerStructureProcessorType(
                        "bobble_randomizer", BobbleRandomizerProcessor.MAP_CODEC);
        MULTIBLOCK_ASSEMBLY =
                Registrar.registerStructureProcessorType(
                        "multiblock_assembly", MultiblockAssemblyProcessor.MAP_CODEC);
        MAYBE_PLACE =
                Registrar.registerStructureProcessorType(
                        "maybe_place", MaybePlaceProcessor.MAP_CODEC);
        LOOT_PILE =
                Registrar.registerStructureProcessorType("loot_pile", LootPileProcessor.MAP_CODEC);
        WEIGHTED_PLACE =
                Registrar.registerStructureProcessorType(
                        "weighted_place", WeightedPlaceProcessor.MAP_CODEC);
        LOCK_PINS =
                Registrar.registerStructureProcessorType("lock_pins", LockPinProcessor.MAP_CODEC);
        GROUND_HEIGHT =
                Registrar.registerStructureProcessorType(
                        "ground_height", GroundHeightProcessor.MAP_CODEC);
        IMPACT_VILLAGE =
                Registrar.registerStructureProcessorType(
                        "impact_village", ImpactVillageProcessor.MAP_CODEC);
        SPAWNER_ENTITY =
                Registrar.registerStructureProcessorType(
                        "spawner_entity", SpawnerEntityProcessor.MAP_CODEC);
        DUCT_FLUID =
                Registrar.registerStructureProcessorType(
                        "duct_fluid", DuctFluidProcessor.MAP_CODEC);
        MARKER_TABLE =
                Registrar.registerStructureProcessorType(
                        "marker_table", MarkerTableProcessor.MAP_CODEC);
    }
}
