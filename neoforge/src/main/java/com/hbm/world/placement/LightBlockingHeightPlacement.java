// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.placement;

import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public final class LightBlockingHeightPlacement extends PlacementModifier {
    public static final LightBlockingHeightPlacement INSTANCE = new LightBlockingHeightPlacement();
    public static final MapCodec<LightBlockingHeightPlacement> CODEC = MapCodec.unit(INSTANCE);
    private static RegistryHandle<PlacementModifierType<LightBlockingHeightPlacement>> type;

    private LightBlockingHeightPlacement() {}

    public static void register(IRegistrar registrar) {
        type = registrar.registerPlacementModifierType("light_blocking_height", CODEC);
    }

    @Override
    public Stream<BlockPos> getPositions(
            PlacementContext context, RandomSource random, BlockPos origin) {
        int x = origin.getX(), z = origin.getZ();
        int height = WorldgenHeight.lightBlocking(context.getLevel(), x, z);
        return height > context.getMinY() ? Stream.of(new BlockPos(x, height, z)) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type() {
        return type.get();
    }
}
