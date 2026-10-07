// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.blocks.network.pneumatic.PneumoTubeBlock;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.GraphSegment;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class ChunkDuctNodes {
    private static final Predicate<BlockState> DUCT =
            state ->
                    state.getBlock() instanceof FluidDuctBlockBase
                            || state.getBlock() instanceof PneumoTubeBlock;
    private static final Map<ServerLevel, LongLinkedOpenHashSet> REDRAW = new HashMap<>();
    private static final Map<ServerLevel, ReferenceOpenHashSet<BlockEntity>> OWNERS =
            new HashMap<>();

    private ChunkDuctNodes() {}

    public static void init() {
        NeoForgeServerEvents.onChunkLoad(ChunkDuctNodes::onChunkLoad);
        NeoForgeServerEvents.onServerTickPost(ChunkDuctNodes::redraw);
        NeoForgeServerEvents.onServerStopping(
                server -> {
                    REDRAW.clear();
                    OWNERS.clear();
                });
    }

    public static void schedule(ServerLevel level, BlockPos pos) {
        BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
        if (state != null) scheduleKnown(level, pos, state);
    }

    private static void scheduleKnown(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FluidPipeBlock
                || state.getBlock() instanceof PneumoTubeBlock) {
            REDRAW.computeIfAbsent(level, ignored -> new LongLinkedOpenHashSet()).add(pos.asLong());
        }
    }

    public static void schedule(BlockEntity owner) {
        if (owner.getLevel() instanceof ServerLevel level && !owner.isRemoved()) {
            OWNERS.computeIfAbsent(level, ignored -> new ReferenceOpenHashSet<>()).add(owner);
        }
    }

    private static void scheduleAround(ServerLevel level, BlockPos pos, BlockState state) {
        scheduleKnown(level, pos, state);
        for (Direction dir : Direction.VALUES) schedule(level, pos.relative(dir));
    }

    public static void scheduleReload(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (LevelNodeGraph<PipeData> graph : FluidPipeGraph.all(level))
                scheduleReload(level, graph);
            scheduleReload(level, PneumaticNetwork.get(level));
        }
    }

    private static <D> void scheduleReload(ServerLevel level, LevelNodeGraph<D> graph) {
        for (NodeNetwork<D> net : graph.networks()) {
            for (GraphNode<D> node : graph.edgeFaces(net))
                schedule(level, BlockPos.of(node.posKey));
            for (GraphSegment<D> segment : net.segments) {
                for (int i = 0; i < segment.cellCount(); i++) {
                    schedule(level, BlockPos.of(segment.cellAt(i)));
                }
            }
        }
    }

    public static void record(ChunkAccess chunk, BlockPos pos, Identifier fluid) {
        Long2ObjectOpenHashMap<String> fluids = chunk.hbm$ductFluids();
        if (fluids == null) chunk.hbm$setDuctFluids(fluids = new Long2ObjectOpenHashMap<>());
        fluids.put(pos.asLong(), fluid.toString());
        chunk.markUnsaved();
    }

    static void typeFromRecord(ServerLevel level, BlockPos pos, FluidDuctBlockBase duct) {
        LevelChunk chunk = level.getChunkAt(pos);
        Fluid fluid = take(chunk, pos);
        if (fluid == null || !duct.retypable()) return;
        FluidPipeBlock.retypeNode(level, pos.asLong(), fluid);
        FluidPipeBlock.refreshAround(level, pos);
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        Long2ObjectOpenHashMap<String> fluids = chunk.hbm$ductFluids();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(DUCT)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!DUCT.test(state)) continue;
                        long key = pos.set(baseX + x, baseY + y, baseZ + z).asLong();
                        scheduleAround(level, pos, state);
                        if (!(state.getBlock() instanceof FluidDuctBlockBase duct)
                                || state.hasBlockEntity()
                                || FluidPipeGraph.graphAt(level, key) != null) continue;
                        duct.createNodes(level, pos.immutable(), state);
                        Fluid fluid = take(chunk, pos);
                        if (fluid != null && duct.retypable())
                            FluidPipeBlock.retypeNode(level, key, fluid);
                    }
                }
            }
        }
        if (fluids != null && !fluids.isEmpty()) {
            fluids.clear();
            chunk.markUnsaved();
        }
    }

    private static void redraw(MinecraftServer server) {
        if (OWNERS.isEmpty() && REDRAW.isEmpty()) return;
        for (ServerLevel level : server.getAllLevels()) {
            ReferenceOpenHashSet<BlockEntity> owners = OWNERS.remove(level);
            if (owners != null) {
                for (BlockEntity owner : owners) FluidPipeBlock.queueConnections(owner);
            }
            LongLinkedOpenHashSet pending = REDRAW.remove(level);
            if (pending == null) continue;
            for (long key : pending) {
                BlockPos pos = BlockPos.of(key);
                BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
                if (state == null) continue;
                if (state.getBlock() instanceof FluidPipeBlock)
                    FluidPipeBlock.refreshConnections(level, pos);
                else if (state.getBlock() instanceof PneumoTubeBlock)
                    PneumoTubeBlock.refreshAirConnections(level, pos);
            }
        }
    }

    private static @Nullable Fluid take(ChunkAccess chunk, BlockPos pos) {
        Long2ObjectOpenHashMap<String> fluids = chunk.hbm$ductFluids();
        if (fluids == null) return null;
        String id = fluids.remove(pos.asLong());
        if (id == null) return null;
        chunk.markUnsaved();
        return BuiltInRegistries.FLUID.getOptional(Identifier.parse(id)).orElse(null);
    }
}
