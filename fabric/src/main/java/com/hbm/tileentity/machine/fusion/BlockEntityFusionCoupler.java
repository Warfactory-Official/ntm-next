// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFusionCoupler extends BlockEntity
        implements GraphResident, FoldedCoreResident, IFusionPowerReceiver {

    public BlockEntityFusionCoupler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COUPLER.get(), pos, state);
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        Direction rot = dir.getClockWise();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.KLYSTRON,
                        core.offset(rot.getStepX(), 2, rot.getStepZ()),
                        rot),
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(-rot.getStepX(), 2, -rot.getStepZ()),
                        rot.getOpposite()));
    }

    private List<FusionPorts.Port> links() {
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        return links(worldPosition, facing);
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(
            long fusionPower, double neutronPower, float r, float g, float b) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockEntityFusionTorus torus =
                FusionPorts.peer(serverLevel, links().get(0), BlockEntityFusionTorus.class);
        if (torus != null) torus.klystronEnergy += fusionPower;
    }
}
