// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKInlet extends BlockEntityMachineBase
        implements IFluidHandlerMK2, SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, 32_000);

    public BlockEntityRBMKInlet(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_INLET.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        if (!RBMKConfig.getReasimBoilers(level)) return;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos npos = worldPosition.relative(dir);
            if (!(level.getBlockState(npos).getBlock() instanceof RBMKBase)) continue;
            BlockPos core = MultiblockSurface.coreOfAny(level, npos);
            if (core != null && level.getBlockEntity(core) instanceof BlockEntityRBMKBase rbmk) {
                int prov =
                        Math.min(BlockEntityRBMKBase.maxWater - rbmk.reasimWater, water.getFill());
                if (prov > 0) {
                    rbmk.reasimWater += prov;
                    water.setFill(water.getFill() - prov);
                    rbmk.markChanged();
                    markChanged();
                }
            }
        }
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!water.accepts(type) || pressure != water.getPressure()) return 0L;
        return (long) water.getMaxFill() - water.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!water.accepts(type) || pressure != water.getPressure()) return amount;
        int accepted = water.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) markChanged();
        return amount - accepted;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(water::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.water.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.water.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
