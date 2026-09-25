// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.packet.SyncField;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachinePumpElectric extends BlockEntityMachinePumpBase
        implements IEnergyHandlerMK2 {

    public static final long MAX_POWER = 10_000;
    private static final long POWER_PER_OP = 1_000;

    @SyncField(units = 1L << 3)
    public long power;

    public BlockEntityMachinePumpElectric(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.ELECTRIC_PUMP.get(),
                pos,
                state,
                MachineData.PUMP_ELECTRIC_SPEED.get() * 100);
    }

    @Override
    protected boolean canOperate() {
        return power >= POWER_PER_OP && water.getFill() < water.getMaxFill();
    }

    @Override
    protected void operate() {
        power -= POWER_PER_OP;
        water.setFill(
                Math.min(
                        water.getFill() + MachineData.PUMP_ELECTRIC_SPEED.get(),
                        water.getMaxFill()));
        setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", power);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeLong(this.power);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.power = input.readLong();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
