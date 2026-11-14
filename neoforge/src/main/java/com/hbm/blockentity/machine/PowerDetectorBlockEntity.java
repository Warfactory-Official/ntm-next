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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blockentity.GraphResident;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.PowerDetectorBlock;
import com.hbm.uninos.ConnectionPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PowerDetectorBlockEntity extends BlockEntity
        implements BlockEntityTicks, EnergyHandler, GraphResident {
    private long reserve;

    public PowerDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HE_DETECTOR.get(), pos, state);
    }

    @Override
    public void tickServer() {
        boolean powered = reserve > 0;
        if (powered) reserve--;
        var state = getBlockState();
        if (state.getValue(PowerDetectorBlock.POWERED) != powered) {
            level.setBlock(
                    worldPosition,
                    state.setValue(PowerDetectorBlock.POWERED, powered),
                    Block.UPDATE_ALL);
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return reserve;
    }

    @Override
    public void setPower(long stored) {
        reserve = stored;
    }

    @Override
    public long getMaxPower() {
        return 5;
    }

    @Override
    public ConnectionPriority getEnergyTier() {
        return ConnectionPriority.HIGH;
    }
}
