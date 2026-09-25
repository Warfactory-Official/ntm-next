// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.util.ChunkUtil;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public class SatelliteResonator extends Satellite {

    public static final MapCodec<SatelliteResonator> CODEC = MapCodec.unit(SatelliteResonator::new);

    public SatelliteResonator() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.RESONATOR;
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        serverPlayer.stopRiding();
        ChunkUtil.loadForEntity(level, new ChunkPos(x >> 4, z >> 4));
        if (y < 0) y = WorldgenHeight.lightBlocking(level.getChunk(x >> 4, z >> 4), x, z);
        serverPlayer.connection.teleport(x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }
}
