// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.placement;

import com.hbm.data.DataGroups;
import com.hbm.data.WorldData;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class WorldConfigPlacement extends PlacementFilter {
    public static final MapCodec<WorldConfigPlacement> CODEC =
            WorldData.WORLDGEN_SWITCH
                    .listOf()
                    .fieldOf("switches")
                    .xmap(WorldConfigPlacement::new, placement -> placement.switches);
    private static RegistryHandle<PlacementModifierType<WorldConfigPlacement>> type;

    private final List<DataGroups.Param<Boolean>> switches;

    public WorldConfigPlacement(List<DataGroups.Param<Boolean>> switches) {
        this.switches = List.copyOf(switches);
    }

    public static void register(IRegistrar registrar) {
        type = registrar.registerPlacementModifierType("world_config", CODEC);
    }

    public boolean open() {
        for (DataGroups.Param<Boolean> gate : switches) {
            if (!gate.get()) return false;
        }
        return true;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return open();
    }

    @Override
    public PlacementModifierType<?> type() {
        return type.get();
    }
}
