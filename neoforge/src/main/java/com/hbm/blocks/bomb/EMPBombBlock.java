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

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EMPBlastEntity;
import com.hbm.explosion.ElectromagneticPulse;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class EMPBombBlock extends Block implements TriggerHandler {

    public EMPBombBlock(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            trigger(level, pos, null);
        }
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {

        level.playSound(
                null,
                pos,
                ModSounds.GUN_SPARK_SHOOT.get(),
                SoundSource.BLOCKS,
                5.0F,
                level.getRandom().nextFloat() * 0.2F + 0.9F);

        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

            ElectromagneticPulse.sphere(level, pos, 50);

            EMPBlastEntity wave = new EMPBlastEntity(ModEntities.EMP_BLAST.get(), level);
            wave.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            wave.setMaxAge(50);
            level.addFreshEntity(wave);
        }

        return TriggerResult.DETONATED;
    }
}
