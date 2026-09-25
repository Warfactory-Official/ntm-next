// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

public abstract class ParticleRBMKJet extends Particle {

    private final RbmkJetShape shape;

    protected ParticleRBMKJet(ClientLevel level, double x, double y, double z, RbmkJetShape shape) {
        super(level, x, y, z);
        this.shape = shape;
    }

    public RbmkJetShape shape() {
        return this.shape;
    }

    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    public int clampedAge() {
        return Math.min(this.age, this.lifetime);
    }
}
