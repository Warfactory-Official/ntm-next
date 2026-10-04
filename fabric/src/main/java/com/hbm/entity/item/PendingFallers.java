// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.interfaces.injected.IChunkExtension;
import com.hbm.packet.ChunkTrackerIndex;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PendingFallers {
    private PendingFallers() {}

    public static boolean canDefer(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkHolder holder =
                level.getChunkSource()
                        .chunkMap
                        .getVisibleChunkIfPresent(ChunkPos.pack(chunkX, chunkZ));
        return holder != null
                && !holder.getFullStatus().isOrAfter(FullChunkStatus.ENTITY_TICKING)
                && !level.isPositionEntityTicking(pos)
                && !ChunkTrackerIndex.isTracked(level, chunkX, chunkZ);
    }

    public static void defer(LevelChunk chunk, BlockPos pos, BlockState state) {
        assert chunk.getPos().x() == (pos.getX() >> 4) && chunk.getPos().z() == (pos.getZ() >> 4);
        int size = chunk.hbm$pendingFallerSize();
        long[] positions = chunk.hbm$pendingFallerPositions();
        BlockState[] states = chunk.hbm$pendingFallerStates();
        if (positions == null) {
            positions = new long[16];
            states = new BlockState[16];
        } else if (size == positions.length) {
            positions = Arrays.copyOf(positions, size << 1);
            states = Arrays.copyOf(states, size << 1);
        }
        positions[size] = pos.asLong();
        states[size] = state;
        chunk.hbm$setPendingFallers(positions, states, size + 1);
        chunk.markUnsaved();
    }

    public static void promote(ServerLevel level, LevelChunk chunk) {
        int size = chunk.hbm$pendingFallerSize();
        if (size == 0) return;
        long[] positions = chunk.hbm$pendingFallerPositions();
        BlockState[] states = chunk.hbm$pendingFallerStates();
        EntityFallingBlockBatch batch = null;
        for (int i = 0; i < size; i++) {
            if (EntityFallingBlockBatch.eligible(states[i])) {
                if (batch != null && batch.fallerCount() == EntityFallingBlockBatch.MAX_CHILDREN) {
                    if (!level.addFreshEntity(batch)) {
                        throw new IllegalStateException(
                                "Could not promote fallout faller batch at " + chunk.getPos());
                    }
                    batch = null;
                }
                if (batch == null) batch = EntityFallingBlockBatch.create(level, chunk);
                batch.add(BlockPos.of(positions[i]), states[i]);
            } else {
                EntityFallingBlockNT falling =
                        EntityFallingBlockNT.fall(level, BlockPos.of(positions[i]), states[i]);
                falling.dropItem = false;
            }
        }
        if (batch != null && !level.addFreshEntity(batch)) {
            throw new IllegalStateException(
                    "Could not promote fallout faller batch at " + chunk.getPos());
        }
        chunk.hbm$setPendingFallers(null, null, 0);
        chunk.markUnsaved();
    }

    public static ListTag save(long[] positions, BlockState[] states, int size) {
        ListTag list = new ListTag();
        for (int i = 0; i < size; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", positions[i]);
            entry.put("state", NbtUtils.writeBlockState(states[i]));
            list.add(entry);
        }
        return list;
    }

    public static void load(ListTag list, IChunkExtension chunk) {
        int size = list.size();
        if (size == 0) return;
        long[] positions = new long[size];
        BlockState[] states = new BlockState[size];
        for (int i = 0; i < size; i++) {
            CompoundTag entry = list.getCompound(i).orElseThrow();
            positions[i] = entry.getLong("pos").orElseThrow();
            CompoundTag stateTag = entry.getCompound("state").orElseThrow();
            states[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, stateTag);
            if (!NbtUtils.writeBlockState(states[i]).equals(stateTag)) {
                throw new IllegalStateException(
                        "Invalid pending faller block state at " + BlockPos.of(positions[i]));
            }
        }
        chunk.hbm$setPendingFallers(positions, states, size);
    }
}
