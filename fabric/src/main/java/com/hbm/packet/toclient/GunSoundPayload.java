// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.config.GunVisualConfig;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class GunSoundPayload extends ThreadedPayload {
    public static final Type<GunSoundPayload> TYPE = new Type<>(Library.id("gun_sound"));
    public static final StreamCodec<ByteBuf, GunSoundPayload> STREAM_CODEC =
            streamCodec(GunSoundPayload::new);

    private final boolean legacy;
    private final Holder<SoundEvent> sound;
    private final SoundSource source;
    private final int x, y, z;
    private final float volume, pitch;
    private final long seed;

    public GunSoundPayload(
            boolean legacy,
            Holder<SoundEvent> sound,
            SoundSource source,
            double x,
            double y,
            double z,
            float volume,
            float pitch,
            long seed) {
        this.legacy = legacy;
        this.sound = sound;
        this.source = source;

        this.x = (int) (x * 8.0);
        this.y = (int) (y * 8.0);
        this.z = (int) (z * 8.0);
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    private GunSoundPayload(ByteBuf buf) {
        legacy = buf.readBoolean();
        sound = SoundEvent.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        source = ((RegistryFriendlyByteBuf) buf).readEnum(SoundSource.class);
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        volume = buf.readFloat();
        pitch = buf.readFloat();
        seed = buf.readLong();
    }

    public static void send(
            ServerLevel level,
            boolean legacy,
            Holder<SoundEvent> sound,
            SoundSource source,
            double x,
            double y,
            double z,
            float volume,
            float pitch,
            long seed) {
        Services.NETWORK.sendToAllAround(
                new GunSoundPayload(legacy, sound, source, x, y, z, volume, pitch, seed),
                new TargetPoint(level, x, y, z, sound.value().getRange(volume)));
    }

    public static void handleClient(GunSoundPayload payload, IPayloadHandlerContext context) {
        if (GunVisualConfig.legacyAnimations != payload.legacy) return;
        var player = context.playerOrNull();
        if (player == null) return;
        player.level()
                .playSeededSound(
                        player,
                        payload.x / 8.0F,
                        payload.y / 8.0F,
                        payload.z / 8.0F,
                        payload.sound,
                        payload.source,
                        payload.volume,
                        payload.pitch,
                        payload.seed);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        RegistryFriendlyByteBuf reg =
                new RegistryFriendlyByteBuf(
                        buf, Services.SERVER.getCurrentServer().registryAccess());
        reg.writeBoolean(legacy);
        SoundEvent.STREAM_CODEC.encode(reg, sound);
        reg.writeEnum(source);
        reg.writeInt(x);
        reg.writeInt(y);
        reg.writeInt(z);
        reg.writeFloat(volume);
        reg.writeFloat(pitch);
        reg.writeLong(seed);
    }

    @Override
    public Type<GunSoundPayload> type() {
        return TYPE;
    }
}
