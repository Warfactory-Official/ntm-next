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

import com.hbm.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class NukeFleijaBlock extends NukeAssemblyBlock {

    public NukeFleijaBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeFleijaBlockEntity(pos, state);
    }

    @Override
    protected void ignite(Level level, BlockPos pos, int yield, @Nullable Entity detonator) {
        NukeExplosionMK3Entity blast =
                NukeExplosionMK3Entity.statFacFleija(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, yield);
        if (blast.isRemoved()) return;

        playBoom(level, pos);
        level.addFreshEntity(blast);
        level.addFreshEntity(
                CloudFleijaEntity.statFac(level, yield, pos.getX(), pos.getY(), pos.getZ()));
    }
}
