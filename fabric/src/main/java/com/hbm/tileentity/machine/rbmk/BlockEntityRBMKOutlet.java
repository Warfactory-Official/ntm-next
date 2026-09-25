// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKOutlet extends BlockEntityMachineBase
        implements FluidFlushSender, SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.SUPERHOTSTEAM, 32_000);

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending = {steam};

    public BlockEntityRBMKOutlet(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_OUTLET.get(), pos, state, 0);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void tickServer() {
        if (RBMKConfig.getReasimBoilers(level)) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos npos = worldPosition.relative(dir);
                if (!(level.getBlockState(npos).getBlock() instanceof RBMKBase)) continue;
                BlockPos core = MultiblockSurface.coreOfAny(level, npos);
                if (core != null
                        && level.getBlockEntity(core) instanceof BlockEntityRBMKBase rbmk) {
                    int prov = Math.min(steam.getMaxFill() - steam.getFill(), rbmk.reasimSteam);
                    if (prov > 0) {
                        rbmk.reasimSteam -= prov;
                        steam.setFill(steam.getFill() + prov);
                        rbmk.markChanged();
                        markChanged();
                    }
                }
            }
        }

        flush.provide((ServerLevel) level, this);
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (pressure != steam.getPressure() || !steam.provides(type)) return 0L;
        return steam.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!steam.provides(type) || pressure != steam.getPressure()) return;
        steam.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        markChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(steam::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        steam.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.steam.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.steam.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
