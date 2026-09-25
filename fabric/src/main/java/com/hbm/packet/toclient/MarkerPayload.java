// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.render.AssemblyMarkers;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.util.GameTime;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class MarkerPayload extends ThreadedPayload {

    public static final Type<MarkerPayload> TYPE = new Type<>(Library.id("marker"));
    public static final StreamCodec<ByteBuf, MarkerPayload> STREAM_CODEC =
            streamCodec(MarkerPayload::decode);

    private final int color;
    private final int ttlMillis;
    private final double maxDist;
    private final Map<BlockPos, Component> markers;

    public MarkerPayload(
            int color, int ttlMillis, double maxDist, Map<BlockPos, Component> markers) {
        this.color = color;
        this.ttlMillis = ttlMillis;
        this.maxDist = maxDist;
        this.markers = markers;
    }

    private static MarkerPayload decode(ByteBuf buf) {
        FriendlyByteBuf b = new FriendlyByteBuf(buf);
        int color = b.readInt();
        int ttl = b.readVarInt();
        double dist = b.readDouble();
        int n = b.readVarInt();
        Map<BlockPos, Component> markers = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            markers.put(
                    b.readBlockPos(),
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(b));
        }
        return new MarkerPayload(color, ttl, dist, markers);
    }

    public static void handle(MarkerPayload payload, IPayloadHandlerContext ctx) {
        long expireAt = payload.ttlMillis > 0 ? GameTime.millis() + payload.ttlMillis : 0;
        AssemblyMarkers.Marker[] markers = new AssemblyMarkers.Marker[payload.markers.size()];
        int i = 0;
        for (var entry : payload.markers.entrySet()) {
            markers[i++] =
                    new AssemblyMarkers.Marker(
                            entry.getKey(),
                            entry.getValue(),
                            expireAt,
                            payload.maxDist,
                            payload.color);
        }
        AssemblyMarkers.queue(markers);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        FriendlyByteBuf b = new FriendlyByteBuf(buf);
        b.writeInt(color);
        b.writeVarInt(ttlMillis);
        b.writeDouble(maxDist);
        b.writeVarInt(markers.size());
        for (var entry : markers.entrySet()) {
            b.writeBlockPos(entry.getKey());
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(b, entry.getValue());
        }
    }

    @Override
    public @NotNull Type<MarkerPayload> type() {
        return TYPE;
    }
}
