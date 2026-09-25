// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.packet.SyncField;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.machine.pile.BlockEntityPileCore.PileChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityPileDeviceBase extends BlockEntityMachineBase {
    @SyncField(units = 1L << 2)
    public int channelNumber;

    protected BlockEntityPileDeviceBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public Direction orientation() {
        return getBlockState().getValue(BlockPileDevice.FACING);
    }

    protected @Nullable PileChannel attachedChannel(BlockPile.Role role) {
        BlockPos entry =
                role == BlockPile.Role.CONTROL
                        ? worldPosition.below()
                        : worldPosition.relative(orientation().getOpposite());
        BlockState state = level.getBlockState(entry);
        if (!state.is(ModBlocks.PILE_BLOCK.get()) || state.getValue(BlockPile.ROLE) != role)
            return null;
        BlockPos owner = AssembledMembers.owner((ServerLevel) level, entry);
        if (owner == null || !(level.getBlockEntity(owner) instanceof BlockEntityPileCore core))
            return null;
        PileChannel channel =
                role == BlockPile.Role.FUEL_IN
                        ? core.getFuelChannel(entry)
                        : role == BlockPile.Role.AIR_IN
                                ? core.getVentilationChannel(entry)
                                : core.getControlChannel(entry);
        if (channel != null) channelNumber = core.getChannelList(channel.type).indexOf(channel);
        return channel;
    }
}
