// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import java.util.List;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;

public record WeightedOption(BlockState state, int weight) {

    public WeightedOption {
        if (weight <= 0)
            throw new IllegalArgumentException("weight must be positive, got " + weight);
    }

    public static WeightedOption of(BlockState state) {
        return new WeightedOption(state, 1);
    }

    public static WeightedList<BlockState> table(List<WeightedOption> options) {
        WeightedList.Builder<BlockState> out = WeightedList.builder();
        for (WeightedOption option : options) out.add(option.state(), option.weight());
        return out.build();
    }
}
