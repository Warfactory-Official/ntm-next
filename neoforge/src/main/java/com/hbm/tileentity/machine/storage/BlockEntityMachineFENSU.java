// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineFENSU extends BlockEntityMachineBattery {

    public static final long MAX_TRANSFER = 10_000_000_000_000_000L;

    public float rotation;
    public float prevRotation;

    public BlockEntityMachineFENSU(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FENSU.get(), pos, state);
    }

    @Override
    public long getMaxPower() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode();
        return mode == MODE_OUTPUT || mode == MODE_BUFFER ? MAX_TRANSFER : 0L;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode();
        return mode == MODE_INPUT || mode == MODE_BUFFER ? MAX_TRANSFER : 0L;
    }

    @Override
    public long getPowerRemainingScaled(long i) {
        double powerScaled = (double) power / (double) getMaxPower();
        return (long) (i * powerScaled);
    }

    @Override
    protected long averagePower(long current, long previous) {
        return current / 2 + previous / 2;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        long overshoot = 0;

        if (power > MAX_TRANSFER) {
            overshoot += power - MAX_TRANSFER;
            power = MAX_TRANSFER;
        }

        long freespace = getMaxPower() - getPower();

        if (freespace < power) {
            overshoot += power - freespace;
            power = freespace;
        }

        if (!simulate) setPower(getPower() + power);

        return overshoot;
    }

    @Override
    protected void updateBufferNode(ServerLevel server) {}

    public float getSpeed() {
        return (float) Math.pow(Math.log(power * 0.75 + 1) * 0.05F, 5);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        rotation += getSpeed();

        if (rotation >= 360F) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
    }
}
