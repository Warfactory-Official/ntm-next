// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class EntitySounds {

    public record Emission(
            Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {}

    private EntitySounds() {}

    public static void play(
            Entity emitter,
            BlockPos pos,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        play(
                emitter,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                sound,
                source,
                volume,
                pitch);
    }

    public static void play(
            Entity emitter,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch) {
        play(
                emitter,
                x,
                y,
                z,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                source,
                volume,
                pitch);
    }

    public static void play(
            Entity emitter,
            double x,
            double y,
            double z,
            Holder<SoundEvent> sound,
            SoundSource source,
            float volume,
            float pitch) {
        if (!(emitter instanceof ServerPlayer player)) {
            emitter.level().playSound(null, x, y, z, sound, source, volume, pitch);
            return;
        }
        ServerLevel level = player.level();
        Emission emission = new EntitySounds.Emission(sound, source, volume, pitch);
        if (emission == null) return;
        long seed = level.soundSeedGenerator.nextLong();
        level.getServer()
                .getPlayerList()
                .broadcast(
                        player,
                        x,
                        y,
                        z,
                        emission.sound().value().getRange(emission.volume()),
                        level.dimension(),
                        new ClientboundSoundPacket(
                                emission.sound(),
                                emission.source(),
                                x,
                                y,
                                z,
                                emission.volume(),
                                emission.pitch(),
                                seed));
        player.connection.send(
                new ClientboundSoundEntityPacket(
                        emission.sound(),
                        emission.source(),
                        player,
                        emission.volume(),
                        emission.pitch(),
                        seed));
    }
}
