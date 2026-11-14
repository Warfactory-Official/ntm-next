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

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.ExplosionBlockTransform;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorDebris implements ExplosionBlockTransform {

    protected Supplier<BlockState> target;

    public BlockMutatorDebris(BlockState target) {
        this(() -> target);
    }

    public BlockMutatorDebris(Supplier<BlockState> target) {
        this.target = target;
    }

    @Override
    public void afterPass(ComposableExplosion blast, BlockPos pos) {

        Level world = blast.level();
        BlockState targetState = target.get();

        for (Direction dir : Direction.VALUES) {
            BlockState state = world.getBlockState(pos.relative(dir));
            if (state.isSolidRender() && state != targetState) {
                world.setBlock(pos, targetState, 3);
                return;
            }
        }
    }
}
