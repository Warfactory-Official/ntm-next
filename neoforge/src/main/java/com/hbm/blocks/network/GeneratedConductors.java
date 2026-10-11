// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.NuclearTech;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.platform.NeoForgeAttachments;
import com.hbm.platform.NeoForgeChunkData;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class GeneratedConductors {
    private static final int[] EMPTY = new int[0];
    private static final MapCodec<int[]> CODEC =
            Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream).fieldOf("positions");
    public static final Supplier<AttachmentType<int[]>> DATA =
            NeoForgeAttachments.register(
                    Identifier.fromNamespaceAndPath(NuclearTech.MOD_ID, "generated_conductors"),
                    () ->
                            AttachmentType.builder(() -> EMPTY)
                                    .serialize(CODEC)
                                    .copyHandler((value, holder, registries) -> value)
                                    .build());

    private GeneratedConductors() {}

    public static void init() {
        NeoForgeServerEvents.onChunkLoad(GeneratedConductors::onChunkLoad);
    }

    public static boolean isBare(BlockState state) {
        return !state.hasBlockEntity()
                && (state.getBlock() instanceof CableBlock
                        || state.getBlock() instanceof CableConductorBlockBase);
    }

    public static void record(
            WorldGenLevel level,
            BlockPos origin,
            Rotation rotation,
            BoundingBox clip,
            long[] positions) {
        if (!(level instanceof WorldGenRegion)) return;
        ChunkAccess chunk = null;
        int[] packed = null;
        int count = 0;
        for (long local : positions) {
            int localX = BlockPos.getX(local), localZ = BlockPos.getZ(local);
            int x =
                    origin.getX()
                            + switch (rotation) {
                                case CLOCKWISE_90 -> -localZ;
                                case CLOCKWISE_180 -> -localX;
                                case COUNTERCLOCKWISE_90 -> localZ;
                                case NONE -> localX;
                            };
            int z =
                    origin.getZ()
                            + switch (rotation) {
                                case CLOCKWISE_90 -> localX;
                                case CLOCKWISE_180 -> -localZ;
                                case COUNTERCLOCKWISE_90 -> -localX;
                                case NONE -> localZ;
                            };
            int y = origin.getY() + BlockPos.getY(local);
            if (!clip.isInside(x, y, z)) continue;
            if (chunk == null) {
                chunk = level.getChunk(x >> 4, z >> 4);
                if (chunk.getPersistedStatus() == ChunkStatus.FULL) return;
                packed = new int[positions.length];
            }
            assert chunk.getPos().x() == (x >> 4) && chunk.getPos().z() == (z >> 4);
            packed[count++] = y << 8 | (z & 15) << 4 | x & 15;
        }
        if (count != 0)
            record(chunk, count == packed.length ? packed : Arrays.copyOf(packed, count));
    }

    public static int pack(BlockPos pos) {
        return pos.getY() << 8 | (pos.getZ() & 15) << 4 | pos.getX() & 15;
    }

    public static void record(ChunkAccess chunk, int[] additions) {
        if (additions.length == 0) return;
        Arrays.sort(additions);
        int count = 1;
        for (int i = 1; i < additions.length; i++) {
            if (additions[i] != additions[count - 1]) additions[count++] = additions[i];
        }
        synchronized (chunk) {
            int[] previous = chunk.getExistingDataOrNull(DATA.get());
            if (previous == null) {
                NeoForgeChunkData.set(
                        chunk,
                        DATA,
                        count == additions.length ? additions : Arrays.copyOf(additions, count));
                return;
            }
            int missing = 0, cursor = 0;
            for (int i = 0; i < count; i++) {
                while (cursor < previous.length && previous[cursor] < additions[i]) cursor++;
                if (cursor == previous.length || previous[cursor] != additions[i]) missing++;
            }
            if (missing == 0) return;
            int[] merged = new int[previous.length + missing];
            int oldIndex = 0, newIndex = 0, size = 0;
            while (oldIndex < previous.length || newIndex < count) {
                int value;
                if (oldIndex < previous.length
                        && (newIndex == count || previous[oldIndex] < additions[newIndex])) {
                    value = previous[oldIndex++];
                } else {
                    value = additions[newIndex++];
                }
                if (size == 0 || merged[size - 1] != value) merged[size++] = value;
            }
            assert size == merged.length;
            chunk.setData(DATA.get(), merged);
        }
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int[] positions = ((ChunkAccess) chunk).getExistingDataOrNull(DATA.get());
        if (positions == null) return;
        LevelNodeGraph<CableData> graph = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        for (int packed : positions) {
            pos.set(baseX + (packed & 15), packed >> 8, baseZ + (packed >> 4 & 15));
            BlockState state = chunk.getBlockState(pos);
            if (!isBare(state)) continue;
            if (graph == null) graph = PowerGraph.get(level);
            int mask =
                    state.getBlock() instanceof CableConductorBlockBase conductor
                            ? conductor.nodeMask(state)
                            : CableConductorBlockBase.OPEN_ALL;
            graph.addNode(pos.asLong(), CableData.INSTANCE, mask);
        }
    }
}
