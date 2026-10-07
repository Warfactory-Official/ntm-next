// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.InfoSystem;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

public final class OreDensityPayload extends ThreadedPayload {

    public static final Type<OreDensityPayload> TYPE = new Type<>(Library.id("ore_density"));
    public static final StreamCodec<ByteBuf, OreDensityPayload> STREAM_CODEC =
            streamCodec(OreDensityPayload::decode);

    private final List<Component> rows;
    private final Component summary;
    private final int firstId;
    private final int millis;

    public OreDensityPayload(List<Component> rows, Component summary, int firstId, int millis) {
        this.rows = List.copyOf(rows);
        this.summary = summary;
        this.firstId = firstId;
        this.millis = millis;
    }

    private static OreDensityPayload decode(ByteBuf buffer) {
        int firstId = buffer.readInt();
        int millis = buffer.readInt();
        int count = buffer.readInt();
        List<Component> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) rows.add(decodeComponent(buffer));
        return new OreDensityPayload(rows, decodeComponent(buffer), firstId, millis);
    }

    public static void handle(OreDensityPayload payload, IPayloadHandlerContext context) {
        for (int i = 0; i < payload.rows.size(); i++)
            push(payload.rows.get(i), payload.firstId + i, payload.millis);
        push(payload.summary, payload.firstId + payload.rows.size(), payload.millis);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(firstId);
        buffer.writeInt(millis);
        buffer.writeInt(rows.size());
        for (Component row : rows) encodeComponent(buffer, row);
        encodeComponent(buffer, summary);
    }

    @Override
    public Type<OreDensityPayload> type() {
        return TYPE;
    }

    private static Component decodeComponent(ByteBuf buffer) {
        return ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer);
    }

    private static void encodeComponent(ByteBuf buffer, Component component) {
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, component);
    }

    private static void push(Component component, int id, int millis) {
        InfoSystem.push(new InfoSystem.InfoEntry(component, millis), id);
    }
}
