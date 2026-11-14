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
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ExplosionStandardLayout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class ExplosionEffectStandard implements BlastEffect {

    @Override
    public void emit(
            ComposableExplosion blast,
            ServerLevel level,
            double x,
            double y,
            double z,
            float size) {

        RandomSource rand = level.getRandom();
        level.playSound(
                null,
                x,
                y,
                z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
        PacketWire.sendToAllAround(
                ExplosionStandardLayout.encode(
                        (float) x, (float) y, (float) z, size, blast.getBlockSnapshot()),
                new TargetPoint(level, x, y, z, 250));
    }
}
