// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachinePumpSteam extends BlockEntityMachinePumpBase
        implements IFluidHandlerMK2 {

    private static final int STEAM_PER_OP = 100;

    @SyncField(units = 1L << 3)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.STEAM, 1_000);

    @SyncField(units = 1L << 4)
    public final FluidTankNTM lps = new FluidTankNTM(NTMFluids.SPENTSTEAM, 10);

    private final FluidTankNTM[] sending;

    public BlockEntityMachinePumpSteam(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.STEAM_PUMP.get(),
                pos,
                state,
                MachineData.PUMP_STEAM_SPEED.get() * 100);
        sending = new FluidTankNTM[] {water, lps};
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    protected boolean canOperate() {
        return steam.getFill() >= STEAM_PER_OP
                && lps.getMaxFill() - lps.getFill() > 0
                && water.getFill() < water.getMaxFill();
    }

    @Override
    protected void operate() {
        steam.setFill(steam.getFill() - STEAM_PER_OP);
        lps.setFill(lps.getFill() + 1);
        water.setFill(
                Math.min(water.getFill() + MachineData.PUMP_STEAM_SPEED.get(), water.getMaxFill()));
        setChanged();
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        long fromWater = super.getFluidAvailable(type, pressure);
        if (fromWater > 0) return fromWater;
        if (lps.provides(type) && pressure == lps.getPressure()) return lps.getFill();
        return 0L;
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        super.useUpFluid(type, pressure, amount);
        if (!lps.provides(type) || pressure != lps.getPressure()) return;
        int drained = (int) Math.min(amount, Integer.MAX_VALUE);
        if (lps.drain(drained, true) > 0) setChanged();
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!steam.accepts(type) || pressure != steam.getPressure()) return 0L;
        return (long) steam.getMaxFill() - steam.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!steam.accepts(type) || pressure != steam.getPressure()) return amount;
        int accepted = steam.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {water, steam, lps};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("steam").ifPresent(steam::deserialize);
        input.child("lps").ifPresent(lps::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        steam.serialize(output.child("steam"));
        lps.serialize(output.child("lps"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x18L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> this.steam.packetSerialize(output);
            case 4 -> this.lps.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.steam.packetDeserialize(input);
            case 4 -> this.lps.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
