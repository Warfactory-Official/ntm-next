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
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PumpjackBlockEntity extends OilDrillBaseBlockEntity {

    static final DepositYield YIELD =
            new DepositYield(
                    MachineData.PUMPJACK_OIL_PER_DEPOSIT,
                    MachineData.PUMPJACK_GAS_PER_DEPOSIT_MIN,
                    MachineData.PUMPJACK_GAS_PER_DEPOSIT_MAX,
                    MachineData.PUMPJACK_DRAIN_CHANCE);
    private static final Direction[] VENT_SIDES = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public float angle;
    public float prevAngle;
    public float beamSpeed;

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMPJACK.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pumpjack");
    }

    @Override
    public long getMaxPower() {
        return MachineData.PUMPJACK_MAX_POWER.get();
    }

    @Override
    public long getPowerReq() {
        return MachineData.PUMPJACK_CONSUMPTION.get();
    }

    @Override
    public int getDelay() {
        return MachineData.PUMPJACK_DELAY.get();
    }

    @Override
    protected void onDrill(BlockPos pos) {
        OreVents.vent(
                level, worldPosition, level.getBlockState(pos), PumpjackBlockEntity::ventSides);
    }

    private static void ventSides(Level level, BlockPos core, BlockState gas) {
        for (Direction side : VENT_SIDES) {
            BlockPos cell = core.relative(side);
            if (level.getBlockState(cell).canBeReplaced())
                level.setBlock(cell, gas, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onSuck(BlockPos pos) {
        YIELD.pump(level, pos, tanks[0], tanks[1]);
        markChanged();
    }

    @Override
    protected int reservePrimary(FluidReserve reserve) {
        return (int) (MachineData.PUMPJACK_RESERVE_OIL_MULTIPLIER.get() * reserve.primaryAmount());
    }

    @Override
    protected int reserveSecondary(FluidReserve reserve) {
        return (int)
                (MachineData.PUMPJACK_RESERVE_GAS_MULTIPLIER.get()
                        * super.reserveSecondary(reserve));
    }

    @Override
    protected double reserveDrainMultiplier() {
        return MachineData.PUMPJACK_RESERVE_DRAIN_MULTIPLIER.get();
    }

    private float publishedSpeed() {
        return indicator == 0 ? 5 + 2 * speedLevel + 10 * (overLevel - 1) : 0F;
    }

    @Override
    public void tickClient() {
        prevAngle = angle;
        if (indicator == 0) angle += beamSpeed;
        if (angle >= 360F) {
            angle -= 360F;
            prevAngle -= 360F;
        }
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 5) {
            super.writeSyncUnit(unit, output);
            return;
        }
        output.writeInt(indicator);
        output.writeFloat(publishedSpeed());
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 5) {
            super.readSyncUnit(unit, input);
            return;
        }
        hbm$syncSet$OilDrillBaseBlockEntity$indicator(input.readInt());
        beamSpeed = input.readFloat();
    }
}
