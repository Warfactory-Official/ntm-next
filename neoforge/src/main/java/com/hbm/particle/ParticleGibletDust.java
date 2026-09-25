// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.world.level.block.state.BlockState;

public class ParticleGibletDust extends TerrainParticle {

    public ParticleGibletDust(ClientLevel level, double x, double y, double z, BlockState state) {
        super(level, x, y, z, 0D, 0D, 0D, state);
        this.setParticleSpeed(0D, 0D, 0D);
        this.setLifetime(20 + this.random.nextInt(20));
    }
}
