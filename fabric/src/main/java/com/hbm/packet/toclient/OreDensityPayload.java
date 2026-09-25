// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.InfoSystem;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

public final class OreDensityPayload extends ThreadedPayload {

    public static final Type<OreDensityPayload> TYPE = new Type<>(Library.id("ore_density"));
    public static final StreamCodec<ByteBuf, OreDensityPayload> STREAM_CODEC =
            streamCodec(OreDensityPayload::decode);

    private final Component lightMetal;
    private final Component heavyMetal;
    private final Component rareEarth;
    private final Component actinide;
    private final Component nonMetal;
    private final Component crystalline;
    private final Component summary;
    private final int firstId;
    private final int millis;

    public OreDensityPayload(
            Component lightMetal,
            Component heavyMetal,
            Component rareEarth,
            Component actinide,
            Component nonMetal,
            Component crystalline,
            Component summary,
            int firstId,
            int millis) {
        this.lightMetal = lightMetal;
        this.heavyMetal = heavyMetal;
        this.rareEarth = rareEarth;
        this.actinide = actinide;
        this.nonMetal = nonMetal;
        this.crystalline = crystalline;
        this.summary = summary;
        this.firstId = firstId;
        this.millis = millis;
    }

    private static OreDensityPayload decode(ByteBuf buffer) {
        int firstId = buffer.readInt();
        int millis = buffer.readInt();
        return new OreDensityPayload(
                decodeComponent(buffer),
                decodeComponent(buffer),
                decodeComponent(buffer),
                decodeComponent(buffer),
                decodeComponent(buffer),
                decodeComponent(buffer),
                decodeComponent(buffer),
                firstId,
                millis);
    }

    public static void handle(OreDensityPayload payload, IPayloadHandlerContext context) {
        push(payload.lightMetal, payload.firstId, payload.millis);
        push(payload.heavyMetal, payload.firstId + 1, payload.millis);
        push(payload.rareEarth, payload.firstId + 2, payload.millis);
        push(payload.actinide, payload.firstId + 3, payload.millis);
        push(payload.nonMetal, payload.firstId + 4, payload.millis);
        push(payload.crystalline, payload.firstId + 5, payload.millis);
        push(payload.summary, payload.firstId + 6, payload.millis);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(firstId);
        buffer.writeInt(millis);
        encodeComponent(buffer, lightMetal);
        encodeComponent(buffer, heavyMetal);
        encodeComponent(buffer, rareEarth);
        encodeComponent(buffer, actinide);
        encodeComponent(buffer, nonMetal);
        encodeComponent(buffer, crystalline);
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
