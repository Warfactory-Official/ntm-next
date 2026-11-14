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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record ShellBlockTransform(Supplier<BlockState> target) implements ExplosionBlockTransform {

    public ShellBlockTransform(BlockState target) {
        this(() -> target);
    }

    @Override
    public void afterDestroy(ComposableExplosion blast, BlockState prior, BlockPos pos) {
        var level = blast.level();
        if (!prior.isRedstoneConductor(level, pos)) return;
        double dx = pos.getX() + 0.5 - blast.getX(),
                dy = pos.getY() + 0.5 - blast.getY(),
                dz = pos.getZ() + 0.5 - blast.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) >= blast.radius() - 0.5) {
            level.setBlock(pos, target.get(), Block.UPDATE_ALL);
        }
    }
}
