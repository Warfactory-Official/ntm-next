// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCraneSplitter extends BlockEntityMachineBase implements SyncUnitSchema {

    public static final int MIN_RATIO = 1;
    public static final int MAX_RATIO = 16;

    @SyncField(units = 1L << 0)
    public byte leftRatio = 1;

    @SyncField(units = 1L << 1)
    public byte rightRatio = 1;

    private boolean position;
    private byte remaining;

    public BlockEntityCraneSplitter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_SPLITTER.get(), pos, state, 0);
    }

    public ItemStack[] splitStack(ItemStack stack) {
        int left = 0;
        int right = 0;
        int count = stack.getCount();

        if (remaining <= 0) remaining = position ? rightRatio : leftRatio;

        while (count > 0) {
            int toExtract = Math.min(remaining, count);

            remaining -= toExtract;
            count -= toExtract;
            if (position) right += toExtract;
            else left += toExtract;

            if (remaining <= 0) {
                position = !position;
                remaining = position ? rightRatio : leftRatio;
            }
        }

        setChanged();
        return new ItemStack[] {stack.copyWithCount(left), stack.copyWithCount(right)};
    }

    public void adjustRatio(boolean isLeft, int adjust) {
        if (isLeft) leftRatio = (byte) Mth.clamp(leftRatio + adjust, MIN_RATIO, MAX_RATIO);
        else rightRatio = (byte) Mth.clamp(rightRatio + adjust, MIN_RATIO, MAX_RATIO);
        setChanged();
        networkPackNT(15);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        position = input.getBooleanOr("pos", false);
        remaining = input.getByteOr("count", (byte) 0);

        leftRatio = (byte) Math.max(input.getByteOr("left", (byte) 0), MIN_RATIO);
        rightRatio = (byte) Math.max(input.getByteOr("right", (byte) 0), MIN_RATIO);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("pos", position);
        output.putByte("count", remaining);
        output.putByte("left", leftRatio);
        output.putByte("right", rightRatio);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeByte(this.leftRatio);
            case 1 -> output.writeByte(this.rightRatio);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.leftRatio = input.readByte();
            case 1 -> this.rightRatio = input.readByte();
            default -> throw new IllegalArgumentException();
        }
    }
}
