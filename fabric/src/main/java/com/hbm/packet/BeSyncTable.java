// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.UnitPayload;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BeSyncTable {
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<Entry>>>
            TABLE = new IdentityHashMap<>();

    private BeSyncTable() {}

    public static void refresh(
            ThreadedPayload payload, ServerLevel level, BlockPos pos, int range) {
        refresh(payload, level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range);
    }

    private static void refresh(
            ThreadedPayload payload,
            ServerLevel level,
            double targetX,
            double targetY,
            double targetZ,
            double range) {
        try {
            if (!(payload instanceof UnitPayload unit) || !unit.requiresRecipientBase()) {
                ByteBuf wire = payload.getCompiledBuffer();
                if (!payload.compiledIncludesWirePrefix()) return;
            }
            int x = (int) Math.floor(targetX),
                    y = (int) Math.floor(targetY),
                    z = (int) Math.floor(targetZ);
            long chunkKey =
                    ChunkPos.pack(
                            SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            var positions =
                    TABLE.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                            .computeIfAbsent(chunkKey, ignored -> new Long2ObjectOpenHashMap<>());
            long key = BlockPos.asLong(x, y, z);
            Entry entry = positions.get(key);
            if (entry == null) {
                entry = new Entry();
                positions.put(key, entry);
            } else if (entry.payload != null) entry.payload.release();
            entry.x = targetX;
            entry.y = targetY;
            entry.z = targetZ;
            entry.rangeSq = range * range;
            entry.position = key;
            entry.level = level;
            payload.retain();
            entry.payload = payload;
            entry.inScope.clear();
            var players =
                    ChunkTrackerIndex.players(
                            level,
                            SectionPos.blockToSectionCoord(x),
                            SectionPos.blockToSectionCoord(z));
            for (int i = 0; i < players.size(); i++) heal(entry, players.get(i));
        } finally {
            payload.release();
        }
    }

    public static void refreshBlob(SyncBlob blob, BlockEntity owner, int range) {
        ServerLevel level = (ServerLevel) owner.getLevel();
        BlockPos pos = owner.getBlockPos();
        long chunkKey = ChunkPos.containing(pos).pack();
        var positions =
                TABLE.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(chunkKey, ignored -> new Long2ObjectOpenHashMap<>());
        Entry entry = positions.get(pos.asLong());
        if (entry == null) {
            entry = new Entry();
            entry.x = pos.getX() + 0.5;
            entry.y = pos.getY() + 0.5;
            entry.z = pos.getZ() + 0.5;
            entry.rangeSq = (double) range * range;
            entry.level = level;
            entry.position = pos.asLong();
            positions.put(pos.asLong(), entry);
        }
        entry.blob = blob;
        var players = ChunkTrackerIndex.players(level, pos.getX() >> 4, pos.getZ() >> 4);
        for (int i = 0; i < players.size(); i++) heal(entry, players.get(i));
    }

    public static void evict(ServerLevel level, BlockPos pos) {
        var chunks = TABLE.get(level);
        if (chunks == null) return;
        long chunkKey = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        var positions = chunks.get(chunkKey);
        if (positions == null) return;
        Entry entry = positions.remove(pos.asLong());
        if (entry != null && entry.payload != null) entry.payload.release();
        if (positions.isEmpty()) chunks.remove(chunkKey);
        if (chunks.isEmpty()) TABLE.remove(level);
    }

    public static void pump() {
        ChunkTrackerIndex.pumpMovement();
    }

    static void playerMoved(ServerLevel level, ServerPlayer player, long chunkKey) {
        var positions = positions(level, chunkKey);
        if (positions == null) return;
        for (Entry entry : positions.values()) heal(entry, player);
    }

    static void chunkSent(ServerLevel level, ServerPlayer player, long chunkKey) {
        var positions = positions(level, chunkKey);
        if (positions == null) return;
        for (Entry entry : positions.values()) entry.inScope.add(player.getUUID());
    }

    static void chunkDropped(ServerLevel level, ServerPlayer player, long chunkKey, boolean empty) {
        var positions = positions(level, chunkKey);
        if (positions == null) return;
        if (empty) {
            for (Entry entry : positions.values())
                if (entry.payload != null) entry.payload.release();
            var chunks = TABLE.get(level);
            chunks.remove(chunkKey);
            if (chunks.isEmpty()) TABLE.remove(level);
        } else {
            for (Entry entry : positions.values()) entry.inScope.remove(player.getUUID());
        }
    }

    private static Long2ObjectOpenHashMap<Entry> positions(ServerLevel level, long chunkKey) {
        var chunks = TABLE.get(level);
        return chunks == null ? null : chunks.get(chunkKey);
    }

    public static void forget(UUID player) {
        for (var chunks : TABLE.values()) {
            for (var positions : chunks.values()) {
                for (Entry entry : positions.values()) entry.inScope.remove(player);
            }
        }
    }

    public static void forgetIn(ServerLevel level, UUID player) {
        var chunks = TABLE.get(level);
        if (chunks == null) return;
        for (var positions : chunks.values()) {
            for (Entry entry : positions.values()) entry.inScope.remove(player);
        }
    }

    public static void clear() {
        for (var chunks : TABLE.values()) {
            for (var positions : chunks.values()) {
                for (Entry entry : positions.values())
                    if (entry.payload != null) entry.payload.release();
            }
        }
        TABLE.clear();
    }

    private static void heal(Entry entry, ServerPlayer player) {
        if (entry.blob == null && entry.inScope.contains(player.getUUID())) return;
        double dx = entry.x - player.getX(),
                dy = entry.y - player.getY(),
                dz = entry.z - player.getZ();
        if (dx * dx + dy * dy + dz * dz >= entry.rangeSq) return;
        if (entry.payload != null && entry.inScope.add(player.getUUID())) {
            PacketWire.enqueueSync(player, entry.level, entry.position, entry.payload);
        }
        if (entry.blob != null) entry.blob.enqueue(entry.level, player);
    }

    private static final class Entry {
        final ObjectOpenHashSet<UUID> inScope = new ObjectOpenHashSet<>(4);
        double x, y, z, rangeSq;
        long position;
        ServerLevel level;
        ThreadedPayload payload;
        SyncBlob blob;
    }
}
