// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.registration.IRegistrar;
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

    private HbmStructureProcessors() {}

    public static void register(IRegistrar r) {
        PICK_ONE = r.registerStructureProcessorType("pick_one", PickOneProcessor.MAP_CODEC);
        BOBBLE_RANDOMIZER =
                r.registerStructureProcessorType(
                        "bobble_randomizer", BobbleRandomizerProcessor.MAP_CODEC);
        MULTIBLOCK_ASSEMBLY =
                r.registerStructureProcessorType(
                        "multiblock_assembly", MultiblockAssemblyProcessor.MAP_CODEC);
        MAYBE_PLACE =
                r.registerStructureProcessorType("maybe_place", MaybePlaceProcessor.MAP_CODEC);
        LOOT_PILE = r.registerStructureProcessorType("loot_pile", LootPileProcessor.MAP_CODEC);
        WEIGHTED_PLACE =
                r.registerStructureProcessorType(
                        "weighted_place", WeightedPlaceProcessor.MAP_CODEC);
        LOCK_PINS = r.registerStructureProcessorType("lock_pins", LockPinProcessor.MAP_CODEC);
        GROUND_HEIGHT =
                r.registerStructureProcessorType("ground_height", GroundHeightProcessor.MAP_CODEC);
        IMPACT_VILLAGE =
                r.registerStructureProcessorType(
                        "impact_village", ImpactVillageProcessor.MAP_CODEC);
    }
}
