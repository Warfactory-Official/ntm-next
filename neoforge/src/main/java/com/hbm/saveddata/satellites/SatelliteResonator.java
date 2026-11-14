/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.saveddata.satellites;

import com.hbm.sound.EntitySounds;
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
    public SatelliteType kind() {
        return SatelliteType.RESONATOR;
    }

    @Override
    public void designate(ServerLevel level, Player player, int x, int y, int z) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        serverPlayer.stopRiding();
        ChunkUtil.loadForEntity(level, new ChunkPos(x >> 4, z >> 4));
        if (y == SURFACE) y = WorldgenHeight.lightBlocking(level.getChunk(x >> 4, z >> 4), x, z);
        serverPlayer.connection.teleport(x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }
}
