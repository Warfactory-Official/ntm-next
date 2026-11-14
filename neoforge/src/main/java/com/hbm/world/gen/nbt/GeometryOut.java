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

import com.hbm.itempool.ComponentLoot;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface GeometryOut {

    void block(int x, int y, int z, BlockState state);

    void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState edge, BlockState fill);

    default void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        box(x0, y0, z0, x1, y1, z1, state, state);
    }

    void airBox(int x0, int y0, int z0, int x1, int y1, int z1);

    void selectorBox(int x0, int y0, int z0, int x1, int y1, int z1, List<WeightedOption> options);

    void maybeBox(
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1,
            float probability,
            BlockState edge,
            BlockState fill);

    void randomBlock(int x, int y, int z, List<WeightedOption> options);

    void container(int x, int y, int z, BlockState state, ComponentLoot loot);

    void lockedContainer(int x, int y, int z, BlockState state, ComponentLoot loot, double mod);

    void door(int x, int y, int z, BlockState door, Direction facing, boolean opensRight);

    void randomDoor(int x, int y, int z, BlockState door, Direction facing, boolean opensRight);

    void bobble(int x, int y, int z);

    void maybeBobble(int x, int y, int z, float probability);

    void lootPile(int x, int y, int z, String pool);

    void multiblock(int x, int y, int z, BlockState core);
}
