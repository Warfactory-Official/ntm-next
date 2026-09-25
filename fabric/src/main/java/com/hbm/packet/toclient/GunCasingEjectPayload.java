// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.config.GunVisualConfig;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class GunCasingEjectPayload extends CasingEjectPayload {
    public static final Type<GunCasingEjectPayload> TYPE =
            new Type<>(Library.id("gun_casing_eject"));
    public static final StreamCodec<ByteBuf, GunCasingEjectPayload> STREAM_CODEC =
            streamCodec(GunCasingEjectPayload::new);

    private final boolean legacy;

    public GunCasingEjectPayload(
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
            int nodeLife,
            boolean legacy) {
        super(
                x, y, z, mX, mY, mZ, yaw, pitch, mPitch, mYaw, casing, smoking, smokeLife,
                smokeLift, nodeLife);
        this.legacy = legacy;
    }

    private GunCasingEjectPayload(ByteBuf buf) {
        super(buf);
        legacy = buf.readBoolean();
    }

    public static void handleClient(GunCasingEjectPayload payload, IPayloadHandlerContext context) {
        if (GunVisualConfig.legacyAnimations != payload.legacy) return;
        ClientParticlePayloadHandlers.handle(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeBoolean(legacy);
    }

    @Override
    public Type<GunCasingEjectPayload> type() {
        return TYPE;
    }
}
