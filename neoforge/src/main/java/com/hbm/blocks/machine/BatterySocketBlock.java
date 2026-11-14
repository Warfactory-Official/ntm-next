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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.storage.SocketBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.network.CableConductorBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BatterySocketBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, LookOverlayProvider {
    private static final int NODE_MASK =
            (1 << Direction.NORTH.ordinal())
                    | (1 << Direction.SOUTH.ordinal())
                    | (1 << Direction.WEST.ordinal())
                    | (1 << Direction.EAST.ordinal());

    public BatterySocketBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SocketBlockEntity(pos, state);
    }

    public static BlockPos[] ports(BlockPos core, Direction facing) {
        Direction rot = facing.getClockWise();
        return new BlockPos[] {
            core,
            core.relative(facing.getOpposite()),
            core.relative(rot),
            core.relative(facing.getOpposite()).relative(rot)
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static void mintNodes(ServerLevel level, BlockPos core, BlockState state) {
        for (BlockPos port : ports(core, coreFacing(state))) {
            CableConductorBlock.mintNode(level, port, state, NODE_MASK);
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        for (BlockPos port : ports(pos, coreFacing(state)))
            CableConductorBlock.dropNode(level, port);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean cellsHaveAnalogOutput() {
        return true;
    }

    @Override
    protected boolean comparatorCellAt(int lx, int ly, int lz) {
        return ly == 0;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof SocketBlockEntity socket)
                || socket.getShownItem().isEmpty()) {
            return;
        }
        info.title(socket.getShownItem().getHoverName())
                .charge(socket.getPower(), socket.getMaxPower());
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BATTERY_SOCKET)
                .powerIn()
                .powerOut()
                .items()
                .itemsAtCells()
                .fe();
    }

    @java.lang.Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @java.lang.Override
    public void cellNeighborChanged(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos core,
            net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof SocketBlockEntity) ((SocketBlockEntity) hbm$be).refreshMode();
    }
}
