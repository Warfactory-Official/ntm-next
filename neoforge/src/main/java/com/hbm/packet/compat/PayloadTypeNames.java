// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import io.netty.buffer.Unpooled;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class PayloadTypeNames {
    private static final ConcurrentHashMap<CustomPacketPayload.Type<?>, Names> TYPES =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> SOURCES = new ConcurrentHashMap<>();

    private PayloadTypeNames() {}

    private static Names names(CustomPacketPayload.Type<?> type) {
        return TYPES.computeIfAbsent(
                type,
                key -> {
                    String channel = key.id().toString();
                    var buffer = Unpooled.buffer();
                    try {
                        new FriendlyByteBuf(buffer).writeUtf(channel);
                        byte[] wire = new byte[buffer.readableBytes()];
                        buffer.readBytes(wire);
                        SOURCES.put(channel, "custom_payload:" + channel);
                        return new Names(channel, wire);
                    } finally {
                        buffer.release();
                    }
                });
    }

    public static String channel(CustomPacketPayload.Type<?> type) {
        return names(type).channel;
    }

    public static byte[] wire(CustomPacketPayload.Type<?> type) {
        return names(type).wire;
    }

    public static String sourceKey(String channel) {
        if (!channel.startsWith("hbm:")) return "custom_payload:" + channel;
        String cached = SOURCES.get(channel);
        return cached == null ? "custom_payload:" + channel : cached;
    }

    private record Names(String channel, byte[] wire) {}
}
