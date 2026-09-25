// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;

public class ParticleRBMKSteam extends ParticleRBMKJet {

    public static final ParticleRenderType RBMK_STEAM_GROUP =
            new ParticleRenderType("hbm:rbmk_steam", "HRS");

    public ParticleRBMKSteam(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, new RbmkJetShape.Steam());
        this.lifetime = RbmkJetShape.Steam.LIFETIME;
    }

    @Override
    public ParticleRenderType getGroup() {
        return RBMK_STEAM_GROUP;
    }
}
