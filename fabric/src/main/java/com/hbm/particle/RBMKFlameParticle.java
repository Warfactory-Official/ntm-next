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

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;

public class RBMKFlameParticle extends RBMKJetParticle {

    public static final ParticleRenderType RBMK_FLAME_GROUP =
            new ParticleRenderType("hbm:rbmk_flame", "HRF");

    public RBMKFlameParticle(ClientLevel level, double x, double y, double z, int maxAge) {
        super(level, x, y, z, new RbmkJetShape.Flame(level.getRandom().nextFloat() + 1F, maxAge));
        this.lifetime = maxAge;
    }

    @Override
    public ParticleRenderType getGroup() {
        return RBMK_FLAME_GROUP;
    }
}
