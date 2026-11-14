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
import com.hbm.particle.helper.ExplosionSmallCreator;
import net.minecraft.server.level.ServerLevel;

public record SmallExplosionEffect(int clouds, float cloudScale, float cloudSpeed)
        implements BlastEffect {

    @Override
    public void emit(
            ComposableExplosion blast,
            ServerLevel level,
            double x,
            double y,
            double z,
            float size) {
        ExplosionSmallCreator.composeEffect(level, x, y, z, clouds, cloudScale, cloudSpeed);
    }
}
