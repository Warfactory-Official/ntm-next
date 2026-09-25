// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.tileentity.network.BlockEntityPylonBase;
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

public abstract class CableConductorBlockBase extends Block {

    public static final int OPEN_ALL = 0x3F;

    protected CableConductorBlockBase(Properties props) {
        super(props);
    }

    protected int nodeMask(BlockState state) {
        return OPEN_ALL;
    }

    public boolean canConnectCable(BlockState state, Direction side) {
        return (nodeMask(state) & (1 << side.ordinal())) != 0;
    }

    public static void mintNode(ServerLevel level, BlockPos pos, BlockState state, int mask) {
        PowerGraph.get(level)
                .addNode(pos.asLong(), PowerGraphProvider.INSTANCE.createData(state), mask);
    }

    public static void dropNode(ServerLevel level, BlockPos pos) {
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        GraphNode<CableData> node = graph.getNode(pos.asLong());
        long[] peers =
                node != null && node.hasRemoteLinks()
                        ? node.remoteLinks.toLongArray()
                        : new long[0];
        graph.removeNode(pos.asLong());

        for (long peer : peers) BlockEntityPylonBase.refreshIfLoaded(level, peer);
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
