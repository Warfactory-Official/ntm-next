// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public sealed class CasingEjectPayload extends ThreadedPayload permits GunCasingEjectPayload {

    public static final Type<CasingEjectPayload> TYPE = new Type<>(Library.id("casing_eject"));
    public static final StreamCodec<ByteBuf, CasingEjectPayload> STREAM_CODEC =
            streamCodec(CasingEjectPayload::new);

    final double x, y, z;
    final double mX, mY, mZ;
    final float yaw, pitch;
    final float mPitch, mYaw;
    final String casing;
    final boolean smoking;
    final int smokeLife;
    final double smokeLift;
    final int nodeLife;

    public CasingEjectPayload(
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            float yaw,
            float pitch,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.mX = mX;
        this.mY = mY;
        this.mZ = mZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.mPitch = mPitch;
        this.mYaw = mYaw;
        this.casing = casing;
        this.smoking = smoking;
        this.smokeLife = smokeLife;
        this.smokeLift = smokeLift;
        this.nodeLife = nodeLife;
    }

    protected CasingEjectPayload(ByteBuf buf) {
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        mX = buf.readDouble();
        mY = buf.readDouble();
        mZ = buf.readDouble();
        yaw = buf.readFloat();
        pitch = buf.readFloat();
        mPitch = buf.readFloat();
        mYaw = buf.readFloat();
        casing = ByteBufCodecs.STRING_UTF8.decode(buf);
        smoking = buf.readBoolean();
        smokeLife = buf.readInt();
        smokeLift = buf.readDouble();
        nodeLife = buf.readInt();
    }

    public static void handleClient(CasingEjectPayload payload, IPayloadHandlerContext ctx) {
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(mX);
        buf.writeDouble(mY);
        buf.writeDouble(mZ);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(mPitch);
        buf.writeFloat(mYaw);
        ByteBufCodecs.STRING_UTF8.encode(buf, casing);
        buf.writeBoolean(smoking);
        buf.writeInt(smokeLife);
        buf.writeDouble(smokeLift);
        buf.writeInt(nodeLife);
    }

    @Override
    public @NotNull Type<? extends CasingEjectPayload> type() {
        return TYPE;
    }
}
