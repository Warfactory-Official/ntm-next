// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.InfoSystem;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class PlayerInformPayload extends ThreadedPayload {

    public static final Type<PlayerInformPayload> TYPE = new Type<>(Library.id("player_inform"));
    public static final StreamCodec<ByteBuf, PlayerInformPayload> STREAM_CODEC =
            streamCodec(PlayerInformPayload::decode);

    public static final int ID_DETONATOR = 8;

    public static final int ID_TOOL_ABILITY = 11;

    public static final int ID_GAS_HAZARD = 12;

    public static final int DEFAULT_MILLIS = 1_000;

    private final Component message;
    private final int id;
    private final int millis;

    public PlayerInformPayload(String message, int id, int millis) {
        this(Component.literal(message), id, millis);
    }

    public PlayerInformPayload(Component message, int id, int millis) {
        this.message = message;
        this.id = id;
        this.millis = millis;
    }

    private static PlayerInformPayload decode(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        return new PlayerInformPayload(
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf),
                friendly.readInt(),
                friendly.readInt());
    }

    public static void handle(PlayerInformPayload payload, IPayloadHandlerContext context) {
        InfoSystem.push(new InfoSystem.InfoEntry(payload.message, payload.millis), payload.id);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, message);
        friendly.writeInt(id);
        friendly.writeInt(millis);
    }

    @Override
    public @NotNull Type<PlayerInformPayload> type() {
        return TYPE;
    }
}
