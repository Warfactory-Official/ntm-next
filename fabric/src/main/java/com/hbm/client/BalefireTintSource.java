// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import java.awt.*;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BalefireTintSource implements BlockTintSource {

    public static final BalefireTintSource INSTANCE = new BalefireTintSource();

    private BalefireTintSource() {}

    @Override
    public int color(BlockState state) {
        return Color.HSBtoRGB(0F, 0F, 1F - state.getValue(FireBlock.AGE) / 30F);
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(FireBlock.AGE);
    }
}
