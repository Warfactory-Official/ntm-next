// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.UnitPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.Connection;
import net.minecraft.network.VarInt;
import net.minecraft.network.Varint21LengthFieldPrepender;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PacketWire {

    private static final Map<ServerPlayer, OutboundBatch> PENDING = new IdentityHashMap<>();
    private static OutboundBatch pendingHead;
    private static volatile byte[] ccpPacketIdBytes;

    private static final class Transport {
        static final boolean EXTENDED_CHUNK_CACHE = Services.PLATFORM.isModLoaded("neb-fabric");
        static final boolean MANAGED =
                Services.PLATFORM.isModLoaded("bandwidthoptimizer") || EXTENDED_CHUNK_CACHE;
    }

    public static boolean managedTransport() {
        return Transport.MANAGED;
    }

    static boolean extendedChunkCache() {
        return Transport.EXTENDED_CHUNK_CACHE;
    }

    private PacketWire() {}

    public static byte[] ccpPacketIdBytes() {
        return ccpPacketIdBytes;
    }

    public static void onServerStarting(MinecraftServer server) {
        ccpPacketIdBytes = null;
        GameProtocols.CLIENTBOUND_TEMPLATE.details().listPackets(PacketWire::recordCcpPacketId);
        assert ccpPacketIdBytes != null;
        discardPending();
        SyncWire.clear();
        ThreadedPayload.clearCompression();
        ChunkTrackerIndex.clear();
        BeSyncTable.clear();
    }

    private static void recordCcpPacketId(PacketType<?> type, int networkId) {
        if (type != CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD) return;
        assert ccpPacketIdBytes == null;
        byte[] bytes = new byte[VarInt.getByteSize(networkId)];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (networkId & 127);
            networkId >>>= 7;
            if (networkId != 0) bytes[i] |= (byte) 128;
        }
        ccpPacketIdBytes = bytes;
    }

    public static void onServerStopped() {
        ccpPacketIdBytes = null;
        discardPending();
        SyncWire.clear();
        ThreadedPayload.clearCompression();
        ChunkTrackerIndex.clear();
        BeSyncTable.clear();
    }

    public static void sendTo(ThreadedPayload payload, ServerPlayer player) {
        sendToPlayers(payload, List.of(player));
    }

    public static void sendToAll(ThreadedPayload payload) {
        MinecraftServer server = Services.SERVER.getCurrentServer();
        if (server == null) {
            payload.release();
            return;
        }
        sendToPlayers(payload, server.getPlayerList().getPlayers());
    }

    public static void sendToDimension(ThreadedPayload payload, ServerLevel level) {
        sendToPlayers(payload, level.players());
    }

    public static void sendToAllAround(ThreadedPayload payload, TargetPoint tp) {
        sendToPlayers(payload, playersAround(tp));
    }

    public static void sendToAllTracking(ThreadedPayload payload, TargetPoint tp) {
        sendToPlayers(
                payload,
                ChunkTrackerIndex.players(
                        tp.level(),
                        SectionPos.blockToSectionCoord(tp.x()),
                        SectionPos.blockToSectionCoord(tp.z())));
    }

    public static void sendToAllTracking(ThreadedPayload payload, Entity entity) {
        if (!(entity.level() instanceof ServerLevel)) {
            payload.release();
            return;
        }
        sendToPlayers(payload, Services.NETWORK.playersWatchingEntity(entity));
    }

    public static boolean hasPlayersTracking(ServerLevel level, BlockPos pos) {
        assert onServerThread();
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        boolean tracked = ChunkTrackerIndex.isTracked(level, chunkX, chunkZ);

        assert extendedChunkCache() || !tracked || anyViewContains(level, chunkX, chunkZ)
                : "chunk tracker index counts "
                        + chunkX
                        + ","
                        + chunkZ
                        + " which no player's view contains";
        return tracked;
    }

    private static boolean anyViewContains(ServerLevel level, int chunkX, int chunkZ) {
        for (ServerPlayer p : level.players()) {
            if (p.getChunkTrackingView().contains(chunkX, chunkZ)) return true;
        }
        return false;
    }

    public static void sendToServer(ThreadedPayload payload) {
        sendToServerClientSide(payload);
    }

    private static void sendToServerClientSide(ThreadedPayload payload) {
        try {
            payload.prepareManaged();
            Packet<?> packet = new ServerboundCustomPayloadPacket(payload);
            Connection c = Minecraft.getInstance().getConnection().getConnection();
            c.send(packet, null, false);
            c.flushChannel();
        } finally {
            payload.release();
        }
    }

    public static List<ServerPlayer> playersAround(TargetPoint tp) {
        List<ServerPlayer> all = tp.level().players();
        List<ServerPlayer> filtered = new ArrayList<>(all.size());
        double r2 = tp.radius() * tp.radius();
        for (ServerPlayer p : all) {
            double dx = tp.x() - p.getX(), dy = tp.y() - p.getY(), dz = tp.z() - p.getZ();
            if (dx * dx + dy * dy + dz * dz < r2) filtered.add(p);
        }
        return filtered;
    }

    public static void sendToPlayers(ThreadedPayload payload, Collection<ServerPlayer> recipients) {
        try {
            if (recipients.isEmpty()) return;
            assert onServerThread();

            ByteBuf wire = payload.getCompiledBuffer();

            if (!payload.compiledIncludesWirePrefix()) return;
            for (ServerPlayer p : recipients) {
                OutboundBatch batch = OutboundBatch.take(p);
                try {
                    batch.add(payload);
                    batch.dispatch(true);
                } catch (RejectedExecutionException rejected) {
                    boolean open = batch.channel().isOpen();
                    batch.discard();
                    if (open) throw rejected;
                } catch (Throwable failure) {
                    batch.discard();
                    throw failure;
                }
            }
        } finally {
            payload.release();
        }
    }

    public static void sendSyncTracking(ThreadedPayload payload, ServerLevel level, BlockPos pos) {
        try {
            if (!(payload instanceof UnitPayload unit) || !unit.requiresRecipientBase()) {
                ByteBuf wire = payload.getCompiledBuffer();
                if (!payload.compiledIncludesWirePrefix()) return;
            }
            var players = ChunkTrackerIndex.players(level, pos.getX() >> 4, pos.getZ() >> 4);
            for (int i = 0; i < players.size(); i++)
                enqueueSync(players.get(i), level, pos.asLong(), payload);
        } finally {
            payload.release();
        }
    }

    static void enqueueSync(
            ServerPlayer player, ServerLevel level, long position, ThreadedPayload payload) {
        assert onServerThread();
        batch(player).addSync(level, position, payload);
    }

    static ChannelHandlerContext framingContext(Channel channel) {
        ChannelHandlerContext framing = channel.pipeline().context("prepender");
        ChannelHandlerContext compression = channel.pipeline().context("compress");
        return framing != null
                        && framing.handler() instanceof Varint21LengthFieldPrepender
                        && (compression == null
                                || compression.handler() instanceof CompressionEncoder)
                ? framing
                : null;
    }

    private static OutboundBatch batch(ServerPlayer player) {
        OutboundBatch batch = PENDING.get(player);
        if (batch == null) {
            batch = OutboundBatch.take(player);
            batch.next = pendingHead;
            pendingHead = batch;
            PENDING.put(player, batch);
        }
        return batch;
    }

    public static void forget(ServerPlayer player) {
        assert onServerThread();
        OutboundBatch batch = PENDING.remove(player);
        if (batch == null) return;
        if (pendingHead == batch) pendingHead = batch.next;
        else {
            OutboundBatch previous = pendingHead;
            while (previous.next != batch) previous = previous.next;
            previous.next = batch.next;
        }
        batch.discard();
    }

    static void dropSyncChunk(ServerLevel level, ServerPlayer player, long chunkKey) {
        OutboundBatch batch = PENDING.get(player);
        if (batch != null) batch.dropChunk(level, chunkKey);
    }

    public static void dropSyncPosition(ServerLevel level, BlockPos pos) {
        var players = ChunkTrackerIndex.players(level, pos.getX() >> 4, pos.getZ() >> 4);
        for (int i = 0; i < players.size(); i++) {
            OutboundBatch batch = PENDING.get(players.get(i));
            if (batch != null) batch.dropPosition(level, pos.asLong());
        }
    }

    public static void flushPending() {
        if (PENDING.isEmpty()) return;
        assert onServerThread();
        try {
            while (pendingHead != null) {
                OutboundBatch batch = pendingHead;
                pendingHead = batch.next;
                PENDING.remove(batch.player);
                batch.next = null;
                Channel channel = null;
                try {
                    channel = batch.channel();
                    batch.dispatch();
                } catch (RejectedExecutionException rejected) {
                    batch.discard();
                    if (channel == null || channel.isOpen()) throw rejected;
                } catch (Throwable failure) {
                    batch.discard();
                    throw failure;
                }
            }
        } finally {
            discardPending();
        }
    }

    private static void discardPending() {
        while (pendingHead != null) {
            OutboundBatch batch = pendingHead;
            pendingHead = batch.next;
            batch.discard();
        }
        PENDING.clear();
    }

    private static boolean onServerThread() {
        MinecraftServer server = Services.SERVER.getCurrentServer();
        return server == null || server.isSameThread();
    }
}
