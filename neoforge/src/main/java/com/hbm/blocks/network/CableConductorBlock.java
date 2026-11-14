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

package com.hbm.blocks.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blockentity.network.PylonBaseBlockEntity;
import com.hbm.capability.PhysicalConnector;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class CableConductorBlock extends Block implements PhysicalConnector {

    public static final int OPEN_ALL = 0x3F;

    protected CableConductorBlock(Properties props) {
        super(props);
    }

    protected int nodeMask(BlockState state) {
        return OPEN_ALL;
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return (nodeMask(state) & (1 << side.ordinal())) != 0;
    }

    public static void mintNode(ServerLevel level, BlockPos pos, BlockState state, int mask) {
        PowerGraph.get(level).addNode(pos.asLong(), CableData.INSTANCE, mask);
    }

    public static void restoreNode(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CableConductorBlock conductor) {
            mintNode(level, pos, state, conductor.nodeMask(state));
        }
    }

    public static void dropNode(ServerLevel level, BlockPos pos) {
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        GraphNode<CableData> node = graph.getNode(pos.asLong());
        long[] peers =
                node != null && node.hasRemoteLinks()
                        ? node.remoteLinks.toLongArray()
                        : new long[0];
        graph.removeNode(pos.asLong());
        for (long peer : peers) PylonBaseBlockEntity.refreshLoadedEndpoint(level, peer);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this))
            mintNode(sl, pos, state, nodeMask(state));
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        dropNode(level, pos);
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
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
    }
}
