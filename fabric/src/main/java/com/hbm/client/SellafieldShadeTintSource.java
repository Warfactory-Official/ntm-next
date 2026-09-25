// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.blocks.generic.BlockSellafieldSlaked;
import java.awt.*;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class SellafieldShadeTintSource implements BlockTintSource {

    public static final SellafieldShadeTintSource INSTANCE = new SellafieldShadeTintSource();

    private SellafieldShadeTintSource() {}

    @Override
    public int color(BlockState state) {
        int shade = state.getValue(BlockSellafieldSlaked.SHADE);
        return Color.HSBtoRGB(0F, 0F, 1F - shade / 15F);
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(BlockSellafieldSlaked.SHADE);
    }
}
