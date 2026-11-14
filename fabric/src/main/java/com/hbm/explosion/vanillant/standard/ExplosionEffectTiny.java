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

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.BlastEffect;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

public class ExplosionEffectTiny implements BlastEffect {

    private static final double RANGE_SQ = 100D * 100D;

    @Override
    public void emit(
            ComposableExplosion blast,
            ServerLevel level,
            double x,
            double y,
            double z,
            float size) {

        level.playSound(
                null, x, y, z, ModSounds.EXPLOSION_TINY.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
        for (var player : level.players()) {
            if (player.distanceToSqr(x, y, z) < RANGE_SQ) {
                level.sendParticles(
                        player, ParticleTypes.EXPLOSION, true, false, x, y, z, 0, 1.5D, 0D, 0D, 1D);
            }
        }
    }
}
