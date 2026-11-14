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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeGadgetBlockEntity;
import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NukeGadgetBlock extends NukeAssemblyBlock {

    public NukeGadgetBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeGadgetBlockEntity(pos, state);
    }

    @Override
    protected void ignite(Level level, BlockPos pos, int yield, @Nullable Entity detonator) {
        playBoom(level, pos);
        NukeExplosionMK5Entity mk5 =
                NukeExplosionMK5Entity.statFac(
                        level, yield, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (detonator != null) mk5.setDetonator(detonator);
        level.addFreshEntity(mk5);
        NukeTorexEntity.statFac(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, yield);
    }
}
