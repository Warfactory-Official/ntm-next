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

package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity.PileChannel;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.blocks.machine.pile.PileDeviceBlock;
import com.hbm.blocks.multiblock.AssembledMembers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class PileDeviceBaseBlockEntity extends MachineBlockEntity {
    public int channelNumber;

    protected PileDeviceBaseBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public Direction orientation() {
        return getBlockState().getValue(PileDeviceBlock.FACING);
    }

    protected @Nullable PileChannel attachedChannel(PileBlock.Role role) {
        BlockPos entry =
                role == PileBlock.Role.CONTROL
                        ? worldPosition.below()
                        : worldPosition.relative(orientation().getOpposite());
        BlockState state = level.getBlockState(entry);
        if (!state.is(ModBlocks.PILE_BLOCK.get()) || state.getValue(PileBlock.ROLE) != role)
            return null;
        BlockPos owner = AssembledMembers.owner((ServerLevel) level, entry);
        if (owner == null || !(level.getBlockEntity(owner) instanceof PileCoreBlockEntity core))
            return null;
        PileChannel channel =
                role == PileBlock.Role.FUEL_IN
                        ? core.getFuelChannel(entry)
                        : role == PileBlock.Role.AIR_IN
                                ? core.getVentilationChannel(entry)
                                : core.getControlChannel(entry);
        if (channel != null)
            hbm$syncSet$PileDeviceBaseBlockEntity$channelNumber(
                    core.getChannelList(channel.type).indexOf(channel));
        return channel;
    }

    public final void hbm$syncSet$PileDeviceBaseBlockEntity$channelNumber(int next) {
        if (this.channelNumber != next) {
            this.channelNumber = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }
}
