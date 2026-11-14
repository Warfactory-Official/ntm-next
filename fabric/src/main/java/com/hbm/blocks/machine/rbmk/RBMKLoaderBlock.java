/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.machine.rbmk;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.inventory.fluid.trait.HeatableTrait;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.ChunkUtil;
import java.util.function.Supplier;
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

public class RBMKLoaderBlock extends Block implements CapabilityBlock {
    private static final int OUTPUT_FACES = 0x3F & ~(1 << Direction.UP.ordinal());
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().fluidOut().selfProvided();

    public RBMKLoaderBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        if (side == Direction.UP) return NTMFluidProperties.hasTrait(fluid, HeatableTrait.class);
        return NTMFluidProperties.hasTrait(fluid, CoolableTrait.class)
                || fluid == NTMFluids.PERFLUOROMETHYL;
    }

    private static @Nullable RBMKBaseBlockEntity column(
            Level level, BlockPos pos, @Nullable RBMKBaseBlockEntity loading) {
        for (int up = 1; up <= 2; up++) {
            BlockPos at = pos.above(up);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, at);
            if (state == null || state.getBlock() instanceof RBMKLoaderBlock) return null;
            BlockEntity be =
                    loading != null && loading.getBlockPos().equals(at)
                            ? loading
                            : ChunkUtil.blockEntityIfLoaded(level, at);
            if (be instanceof RBMKBaseBlockEntity rbmk
                    && !rbmk.isRemoved()
                    && rbmk instanceof FluidHandler) return rbmk;
        }
        return null;
    }

    private static @Nullable FluidHandler provider(
            Level level, BlockPos pos, @Nullable FluidFace face) {
        if (face != null && face.side() == Direction.UP) return null;
        return (FluidHandler) column(level, pos, null);
    }

    @Override
    public MachineCaps caps() {
        return CAPS;
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.PROVIDER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) -> provider(level, pos, face)));
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
        ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$endpoints().withdraw(pos);
        changed(level, pos);
    }

    public static void refreshBelow(ServerLevel level, RBMKBaseBlockEntity column) {
        for (int down = 1; down <= 2; down++) {
            BlockPos at = column.getBlockPos().below(down);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, at);
            if (state != null && state.getBlock() instanceof RBMKLoaderBlock)
                refresh(level, at, column);
        }
    }

    private static void refresh(
            ServerLevel level, BlockPos pos, @Nullable RBMKBaseBlockEntity loading) {
        RBMKBaseBlockEntity owner = column(level, pos, loading);
        if (owner == null)
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$endpoints()
                    .withdraw(pos);
        else
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$endpoints()
                    .declare(pos, owner.getBlockPos(), OUTPUT_FACES, CAPS.declaredBits());
        changed(level, pos);
    }

    private static void changed(ServerLevel level, BlockPos pos) {
        MultiblockCoreBlock.notifyCell(level, pos);
        FlushIndex.invalidateAt(level, pos.above().asLong());
        FlushIndex.invalidateAt(level, pos.above(2).asLong());
    }
}
