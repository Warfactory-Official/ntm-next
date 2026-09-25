// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRail extends BlockEntity implements Synced, SyncUnitSchema {

    @SyncField(units = 1L)
    public boolean isSwitched = false;

    public BlockEntityRail(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAIL.get(), pos, state);
    }

    public void toggleSwitch() {
        isSwitched = !isSwitched;
        setChanged();
        if (level != null && !level.isClientSide()) networkPackNTTracking();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("isSwitched", isSwitched);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        isSwitched = in.getBooleanOr("isSwitched", false);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeBoolean(isSwitched);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        isSwitched = input.readBoolean();
    }
}
