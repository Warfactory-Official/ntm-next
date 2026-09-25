// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class DebugParticlePayload extends ThreadedPayload {

    public static final Type<DebugParticlePayload> TYPE = new Type<>(Library.id("debug_particle"));
    public static final StreamCodec<ByteBuf, DebugParticlePayload> STREAM_CODEC =
            streamCodec(DebugParticlePayload::decode);

    public static final int MODE_TEXT = 0;
    public static final int MODE_LETTER = 4;
    public static final int MODE_DRONE_LINE = 5;

    final double x, y, z;
    final double mx, my, mz;
    final int mode, color;
    final float scale;
    final String text;

    private DebugParticlePayload(
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            int mode,
            int color,
            float scale,
            String text) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.mx = mx;
        this.my = my;
        this.mz = mz;
        this.mode = mode;
        this.color = color;
        this.scale = scale;
        this.text = text;
    }

    public static DebugParticlePayload text(
            double x, double y, double z, int color, float scale, String text) {
        return new DebugParticlePayload(x, y, z, 0, 0, 0, MODE_TEXT, color, scale, text);
    }

    public static DebugParticlePayload letter(double x, double y, double z, int color, char c) {
        return new DebugParticlePayload(
                x, y, z, 0, 0, 0, MODE_LETTER, color, 1F, String.valueOf(c));
    }

    public static DebugParticlePayload droneLine(
            double x, double y, double z, double mx, double my, double mz, int color) {
        return new DebugParticlePayload(x, y, z, mx, my, mz, MODE_DRONE_LINE, color, 1F, "");
    }

    private static DebugParticlePayload decode(ByteBuf buf) {
        double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
        double mx = buf.readDouble(), my = buf.readDouble(), mz = buf.readDouble();
        int mode = buf.readByte();
        int color = buf.readInt();
        float scale = buf.readFloat();
        int len = buf.readShort();
        byte[] raw = new byte[len];
        buf.readBytes(raw);
        return new DebugParticlePayload(
                x, y, z, mx, my, mz, mode, color, scale, new String(raw, StandardCharsets.UTF_8));
    }

    public static void handleClient(DebugParticlePayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(mx);
        buf.writeDouble(my);
        buf.writeDouble(mz);
        buf.writeByte(mode);
        buf.writeInt(color);
        buf.writeFloat(scale);
        byte[] raw = text.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(raw.length);
        buf.writeBytes(raw);
    }

    @Override
    public @NotNull Type<DebugParticlePayload> type() {
        return TYPE;
    }
}
