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
