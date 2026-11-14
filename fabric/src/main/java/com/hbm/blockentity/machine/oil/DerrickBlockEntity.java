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

package com.hbm.blockentity.machine.oil;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.FluidReserve;
import com.hbm.data.MachineData;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DerrickBlockEntity extends OilDrillBaseBlockEntity {

    static final DepositYield YIELD =
            new DepositYield(
                    MachineData.DERRICK_OIL_PER_DEPOSIT,
                    MachineData.DERRICK_GAS_PER_DEPOSIT_MIN,
                    MachineData.DERRICK_GAS_PER_DEPOSIT_MAX,
                    MachineData.DERRICK_DRAIN_CHANCE);

    public DerrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DERRICK.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.oilWell");
    }

    @Override
    public long getMaxPower() {
        return MachineData.DERRICK_MAX_POWER.get();
    }

    @Override
    public long getPowerReq() {
        return MachineData.DERRICK_CONSUMPTION.get();
    }

    @Override
    public int getDelay() {
        return MachineData.DERRICK_DELAY.get();
    }

    @Override
    protected void onDrill(BlockPos pos) {
        OreVents.vent(
                level, worldPosition, level.getBlockState(pos), DerrickBlockEntity::ventDiagonal);
    }

    private static void ventDiagonal(Level level, BlockPos core, BlockState gas) {
        for (int j = -1; j <= 1; j++) {
            if (!level.getBlockState(core.offset(j, 10, j)).canBeReplaced()) continue;
            for (int k = -1; k <= 1; k++)
                level.setBlock(core.offset(k, 10, k), gas, Block.UPDATE_ALL);
            return;
        }
    }

    @Override
    protected void onSuck(BlockPos pos) {
        splash();
        YIELD.pump(level, pos, tanks[0], tanks[1]);
        markChanged();
    }

    @Override
    protected void onReserveSuck(BlockPos pos, FluidReserve reserve) {
        splash();
        super.onReserveSuck(pos, reserve);
    }

    private void splash() {
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                SoundEvents.GENERIC_SPLASH,
                SoundSource.BLOCKS,
                2.0F,
                0.5F);
    }
}
