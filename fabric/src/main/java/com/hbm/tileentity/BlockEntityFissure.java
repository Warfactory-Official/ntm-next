// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFissure extends BlockEntity implements FluidTankEndpoint {

    private static final int CAPACITY = 1_000;

    private final FluidTankNTM lava = new FluidTankNTM(NTMFluids.LAVA, CAPACITY);
    private final FluidTankNTM[] sending = {lava};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityFissure(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISSURE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityFissure be) {
        be.lava.setFill(CAPACITY);
        be.flush.provide((ServerLevel) level, be);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {

        out.add(lava, (level, pos, contact) -> contact.contact(pos.above(), Direction.DOWN));
    }
}
