// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.data.MachineData;
import com.hbm.packet.SyncField;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCondenserPowered extends BlockEntityCondenser implements IEnergyHandlerMK2 {

    @SyncField(units = 1L << 3)
    public long power;

    public float spin, prevSpin;

    public BlockEntityCondenserPowered(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.CONDENSER_POWERED.get(),
                pos,
                state,
                MachineData.CONDENSER_POWERED_INPUT_TANK_SIZE.get(),
                MachineData.CONDENSER_POWERED_OUTPUT_TANK_SIZE.get());
    }

    @Override
    protected boolean extraCondition(int convert) {
        return power
                >= (long) convert * MachineData.CONDENSER_POWERED_POWER_CONSUMPTION.get() * 0.95D;
    }

    @Override
    protected void postConvert(int convert) {
        power -= (long) convert * MachineData.CONDENSER_POWERED_POWER_CONSUMPTION.get();
        if (power < 0) power = 0;
    }

    @Override
    public void tickClient() {
        prevSpin = spin;

        if (waterTimer > 0) {
            spin += 30F;
            if (spin >= 360F) {
                spin -= 360F;
                prevSpin -= 360F;
            }

            if (TickPhase.every(this, 4)) {
                Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING);
                double x = worldPosition.getX() + 0.5;
                double y = worldPosition.getY() + 1.5;
                double z = worldPosition.getZ() + 0.5;
                level.addParticle(
                        ParticleTypes.CLOUD,
                        x + dir.getStepX() * 1.5,
                        y,
                        z + dir.getStepZ() * 1.5,
                        dir.getStepX() * 0.1,
                        0,
                        dir.getStepZ() * 0.1);
                level.addParticle(
                        ParticleTypes.CLOUD,
                        x - dir.getStepX() * 1.5,
                        y,
                        z - dir.getStepZ() * 1.5,
                        dir.getStepX() * -0.1,
                        0,
                        dir.getStepZ() * -0.1);
            }
        }
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
        return MachineData.CONDENSER_POWERED_MAX_POWER.get();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        power = in.getLongOr("power", power);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putLong("power", power);
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
