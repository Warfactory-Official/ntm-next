// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public abstract class BlockEntityChimneyBase extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IFluidHandlerMK2, SyncUnitSchema {

    private static final long DEMAND = 1_000_000L;

    public static Consumer<BlockEntityChimneyBase> CLIENT_TOWER = be -> {};

    public long ashTick;
    public long sootTick;

    @SyncField(units = 1L << 0)
    public int onTicks;

    protected BlockEntityChimneyBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickServer() {
        if (ashTick > 0 || sootTick > 0) {

            BlockEntity below = level.getBlockEntity(worldPosition.below());
            if (below instanceof BlockEntityAshpit ashpit) {
                ashpit.ashLevelFly += (int) ashTick;
                ashpit.ashLevelSoot += (int) sootTick;
                ashpit.setChanged();
            }
            ashTick = 0;
            sootTick = 0;
        }

        networkPackNT(150);

        if (onTicks > 0) onTicks--;
    }

    public void tickClient() {
        if (onTicks <= 0) return;
        if (!TickPhase.every(this, 2)) return;
        CLIENT_TOWER.accept(this);
    }

    public abstract int plumeHeight();

    public abstract float plumeBaseScale();

    public boolean capturesAsh() {
        return true;
    }

    public boolean capturesSoot() {
        return false;
    }

    public abstract double getPollutionMod();

    @Override
    public long getDemand(Fluid type, int pressure) {
        return DEMAND;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long fluid) {
        if (!FluidPipeGraph.isSmoke(type)) return fluid;

        onTicks = 20;

        if (capturesAsh()) ashTick += fluid;
        if (capturesSoot()) sootTick += fluid;

        fluid = (long) (fluid * getPollutionMod());

        if (type == NTMFluids.SMOKE) {
            PollutionHandler.incrementPollution(
                    level, worldPosition, PollutionType.SOOT, fluid / 100F);
        } else if (type == NTMFluids.SMOKE_LEADED) {
            PollutionHandler.incrementPollution(
                    level, worldPosition, PollutionType.HEAVYMETAL, fluid / 100F);
        } else if (type == NTMFluids.SMOKE_POISON) {
            PollutionHandler.incrementPollution(
                    level, worldPosition, PollutionType.POISON, fluid / 100F);
        }

        return 0;
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        return dir != null && dir.getAxis() != Direction.Axis.Y && FluidPipeGraph.isSmoke(type);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.onTicks);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.onTicks = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
