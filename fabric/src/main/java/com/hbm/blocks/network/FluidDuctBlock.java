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

import com.hbm.api.fluidmk2.*;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.blocks.DebugInfoProvider;
import com.hbm.capability.PhysicalConnector;
import com.hbm.extprop.PlayerState;
import com.hbm.handler.ToolKey;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NetCensus;
import com.hbm.uninos.graph.NodeNetwork;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class FluidDuctBlock extends Block
        implements PhysicalConnector,
                CapabilityDependentShape,
                LookOverlayProvider,
                DebugInfoProvider,
                SettingsHandler {

    public static final int OPEN_ALL = 0x3F;

    protected FluidDuctBlock(Properties props) {
        super(CapabilityDependentShape.deferShapeToFullChunk(props));
    }

    protected static void addTypeLine(Level level, BlockPos pos, LookOverlay info) {
        Fluid type = FluidPipeBlock.pipeFluidAt(level, pos);
        Component name =
                type == Fluids.EMPTY
                        ? Component.translatable("hbmfluid.none")
                        : NTMFluidProperties.getDisplayName(type);
        info.row(name, ARGB.transparent(FluidPipeTintData.colorFor(type)));
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        if (level.isClientSide()) {
            return FluidPipeTintData.fluidAtIfKnown(level, pos) == fluid;
        }
        PipeData data = FluidPipeGraph.get((ServerLevel) level, fluid).dataAt(pos.asLong());
        return data != null && data.fluid() == fluid;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        info.title(getName());
        addTypeLine(level, pos, info);
    }

    @Override
    public @Nullable List<Component> getDebugInfo(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return null;
        Fluid fluid = FluidPipeBlock.pipeFluidAt(level, pos);
        if (fluid == Fluids.EMPTY) return null;

        LevelNodeGraph<PipeData> graph = FluidPipeGraph.get(sl, fluid);
        NodeNetwork<PipeData> net = graph.networkAt(pos.asLong());
        if (net == null) return null;

        NetCensus census = FluidNetwork.census(sl, graph, net);
        return List.of(
                Component.translatable("desc.analysisTool.links", census.links()),
                Component.translatable("desc.analysisTool.subscribers", census.receivers()),
                Component.translatable("desc.analysisTool.providers", census.providers()),
                Component.translatable("desc.analysisTool.transfer", census.transfer()));
    }

    protected int nodeMask(BlockState state) {
        return OPEN_ALL;
    }

    public boolean retypable() {
        return true;
    }

    @Override
    public boolean canCopySettings(Level level, BlockPos pos) {
        return retypable();
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("fluidCount", 1);
        tag.putString(
                "fluidID0",
                BuiltInRegistries.FLUID.getKey(FluidPipeBlock.pipeFluidAt(level, pos)).toString());
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        int count = nbt.getIntOr("fluidCount", 0);
        if (count <= 0 || !(level instanceof ServerLevel server)) return;
        Fluid fluid =
                index < count
                        ? BuiltInRegistries.FLUID.getValue(
                                Identifier.parse(nbt.getStringOr("fluidID" + index, "")))
                        : Fluids.EMPTY;
        if (PlayerState.getOrCreate(player).held(ToolKey.CTRL)) {
            FluidPipeBlock.retypeRun(server, pos.asLong(), fluid);
        } else {
            FluidPipeBlock.retypeNode(server, pos.asLong(), fluid);
            FluidPipeBlock.refreshAround(server, pos);
        }
    }

    @Override
    public String[] getSettingsOptions(Level level, BlockPos pos) {
        Fluid fluid = FluidPipeBlock.pipeFluidAt(level, pos);
        return new String[] {
            fluid == Fluids.EMPTY
                    ? "hbmfluid.none"
                    : NTMFluidProperties.nameKey(BuiltInRegistries.FLUID.getKey(fluid))
        };
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity by,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (level.isClientSide() && retypable()) {
            FluidPipeTintData.set(level, pos.asLong(), FluidPipeBlock.stampedFluid(stack));
            state.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            createNodes(sl, pos, state);
            ChunkDuctNodes.typeFromRecord(sl, pos, this);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        destroyNodes(level, pos);
    }

    protected void createNodes(ServerLevel level, BlockPos pos, BlockState state) {
        long key = pos.asLong();
        if (FluidPipeGraph.graphAt(level, key) != null) return;
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.get(level);
        PipeData data = PipeData.UNSET;
        graph.addNode(key, data, nodeMask(state));
        FluidPipeTintData.sync(level, pos, data.fluid());
    }

    protected void destroyNodes(ServerLevel level, BlockPos pos) {
        long key = pos.asLong();
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(level, key);
        if (graph == null) return;
        GraphNode<PipeData> node = graph.getNode(key);
        long[] links =
                node != null && node.hasRemoteLinks()
                        ? node.remoteLinks.toLongArray()
                        : new long[0];
        graph.removeNode(key);
        for (LevelNodeGraph<PipeData> other : FluidPipeGraph.all(level)) {
            if (other != graph) for (long peer : links) other.forgetLink(peer, key);
        }
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
