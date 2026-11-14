/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.network;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CraneSplitterBlockEntity extends MachineBlockEntity implements SyncUnitSchema {

    public static final int MIN_RATIO = 1;
    public static final int MAX_RATIO = 16;

    public byte leftRatio;

    {
        hbm$syncSet$CraneSplitterBlockEntity$leftRatio((byte) (1));
    }

    public byte rightRatio;

    {
        hbm$syncSet$CraneSplitterBlockEntity$rightRatio((byte) (1));
    }

    private boolean position;
    private byte remaining;

    public CraneSplitterBlockEntity(BlockPos pos, BlockState state) {
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
        if (isLeft)
            hbm$syncSet$CraneSplitterBlockEntity$leftRatio(
                    (byte) Mth.clamp(leftRatio + adjust, MIN_RATIO, MAX_RATIO));
        else
            hbm$syncSet$CraneSplitterBlockEntity$rightRatio(
                    (byte) Mth.clamp(rightRatio + adjust, MIN_RATIO, MAX_RATIO));
        setChanged();
        networkPackNT(15);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        position = input.getBooleanOr("pos", false);
        remaining = input.getByteOr("count", (byte) 0);
        hbm$syncSet$CraneSplitterBlockEntity$leftRatio(
                (byte) Math.max(input.getByteOr("left", (byte) 0), MIN_RATIO));
        hbm$syncSet$CraneSplitterBlockEntity$rightRatio(
                (byte) Math.max(input.getByteOr("right", (byte) 0), MIN_RATIO));
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
            case 0 -> this.hbm$syncSet$CraneSplitterBlockEntity$leftRatio(input.readByte());
            case 1 -> this.hbm$syncSet$CraneSplitterBlockEntity$rightRatio(input.readByte());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$CraneSplitterBlockEntity$leftRatio(byte next) {
        if (this.leftRatio != next) {
            this.leftRatio = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$CraneSplitterBlockEntity$rightRatio(byte next) {
        if (this.rightRatio != next) {
            this.rightRatio = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }
}
