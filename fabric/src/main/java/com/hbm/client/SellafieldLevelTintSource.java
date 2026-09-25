// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.blocks.generic.BlockSellafield;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class SellafieldLevelTintSource implements BlockTintSource {

    public static final SellafieldLevelTintSource INSTANCE = new SellafieldLevelTintSource();

    private static final int[] LEVEL_COLORS = {
        0x4C7939, 0x418223, 0x338C0E, 0x1C9E00, 0x02B200, 0x00D300
    };

    private SellafieldLevelTintSource() {}

    @Override
    public int color(BlockState state) {
        return LEVEL_COLORS[state.getValue(BlockSellafield.LEVEL)];
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(BlockSellafield.LEVEL);
    }
}
