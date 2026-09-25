// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class RBMKLoader extends Block implements ICapabilityBlock {
    private static final int OUTPUT_FACES = 0x3F & ~(1 << Direction.UP.ordinal());
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().fluidOut().selfProvided();

    public RBMKLoader(Properties props) {
        super(props);
    }

    public static boolean canConnectFluid(Direction side, Fluid fluid) {
        if (side == Direction.UP) return NTMFluidProperties.hasTrait(fluid, FT_Heatable.class);
        return NTMFluidProperties.hasTrait(fluid, FT_Coolable.class)
                || fluid == NTMFluids.PERFLUOROMETHYL;
    }

    private static @Nullable BlockEntityRBMKBase column(
            Level level, BlockPos pos, @Nullable BlockEntityRBMKBase loading) {
        for (int up = 1; up <= 2; up++) {
            BlockPos at = pos.above(up);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, at);

            if (state == null || state.getBlock() instanceof RBMKLoader) return null;
            BlockEntity be =
                    loading != null && loading.getBlockPos().equals(at)
                            ? loading
                            : ChunkUtil.blockEntityIfLoaded(level, at);
            if (be instanceof BlockEntityRBMKBase rbmk
                    && !rbmk.isRemoved()
                    && rbmk instanceof IFluidHandlerMK2) return rbmk;
        }
        return null;
    }

    private static @Nullable IFluidHandlerMK2 provider(
            Level level, BlockPos pos, @Nullable FluidFace face) {
        if (face != null && face.side() == Direction.UP) return null;
        return (IFluidHandlerMK2) column(level, pos, null);
    }

    @Override
    public MachineCaps caps() {
        return CAPS;
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Services.CAPS.registerBlockProvider(
                FluidCaps.PROVIDER,
                self,
                (level, pos, state, be, face) -> provider(level, pos, face));
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) refresh(server, pos, null);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel server) refresh(server, pos, null);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        level.hbm$endpoints().withdraw(pos);
        changed(level, pos);
    }

    public static void refreshBelow(ServerLevel level, BlockEntityRBMKBase column) {
        for (int down = 1; down <= 2; down++) {
            BlockPos at = column.getBlockPos().below(down);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, at);
            if (state != null && state.getBlock() instanceof RBMKLoader) refresh(level, at, column);
        }
    }

    private static void refresh(
            ServerLevel level, BlockPos pos, @Nullable BlockEntityRBMKBase loading) {
        BlockEntityRBMKBase owner = column(level, pos, loading);
        if (owner == null) level.hbm$endpoints().withdraw(pos);
        else
            level.hbm$endpoints()
                    .declare(pos, owner.getBlockPos(), OUTPUT_FACES, CAPS.declaredBits());
        changed(level, pos);
    }

    private static void changed(ServerLevel level, BlockPos pos) {
        Services.CAPS.invalidateCaps(level, pos);
        LevelNodeGraph.invalidateEndpointsAround(level, pos);

        FlushIndex.invalidateAt(level, pos.above().asLong());
        FlushIndex.invalidateAt(level, pos.above(2).asLong());
    }
}
