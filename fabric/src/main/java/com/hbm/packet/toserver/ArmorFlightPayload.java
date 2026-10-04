// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.armor.ArmorFlight;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public final class ArmorFlightPayload extends ThreadedPayload {

    public static final Type<ArmorFlightPayload> TYPE = new Type<>(Library.id("armor_flight"));
    public static final StreamCodec<ByteBuf, ArmorFlightPayload> STREAM_CODEC =
            streamCodec(buf -> new ArmorFlightPayload(buf.readUnsignedByte(), buf.readBoolean()));
    private final int mode;
    private final boolean euphemiumFallCapped;

    public ArmorFlightPayload(int mode, boolean euphemiumFallCapped) {
        this.mode = mode;
        this.euphemiumFallCapped = euphemiumFallCapped;
    }

    public static void handle(ArmorFlightPayload payload, IPayloadHandlerContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        boolean valid = payload.mode >= 0 && payload.mode < ArmorFlight.Mode.VALUES.length;
        props.armorFlightIntent = valid ? payload.mode : 0;
        props.euphemiumFallCapped = valid && payload.euphemiumFallCapped;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        buf.writeBoolean(euphemiumFallCapped);
    }

    @Override
    public Type<ArmorFlightPayload> type() {
        return TYPE;
    }
}
