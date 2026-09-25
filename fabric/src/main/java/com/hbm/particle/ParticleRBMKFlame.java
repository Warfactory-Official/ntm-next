// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleRBMKFlame extends ParticleRBMKJet {

    public static final ParticleRenderType RBMK_FLAME_GROUP =
            new ParticleRenderType("hbm:rbmk_flame", "HRF");

    public ParticleRBMKFlame(ClientLevel level, double x, double y, double z, int maxAge) {
        super(level, x, y, z, new RbmkJetShape.Flame(level.getRandom().nextFloat() + 1F, maxAge));
        this.lifetime = maxAge;
    }

    @Override
    public ParticleRenderType getGroup() {
        return RBMK_FLAME_GROUP;
    }
}
