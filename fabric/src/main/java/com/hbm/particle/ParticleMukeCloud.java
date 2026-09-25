// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.lib.Library;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.Identifier;

public class ParticleMukeCloud extends Particle {

    public static final ParticleRenderType MUKE_CLOUD_GROUP =
            new ParticleRenderType("hbm:muke_cloud", "HMC");

    private static final Identifier TEXTURE = Library.id("textures/particle/explosion.png");

    private final float friction;

    public ParticleMukeCloud(
            ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        super(level, x, y, z);
        this.xd = mx;
        this.yd = my;
        this.zd = mz;

        if (yd > 0) {
            this.friction = 0.9F;

            if (yd > 0.1) this.lifetime = 92 + random.nextInt(11) + (int) (yd * 20);
            else this.lifetime = 72 + random.nextInt(11);

        } else if (yd == 0) {

            this.friction = 0.95F;
            this.lifetime = 52 + random.nextInt(11);

        } else {

            this.friction = 0.85F;
            this.lifetime = 122 + random.nextInt(31);
            this.age = 80;
        }
    }

    @Override
    public void tick() {
        this.hasPhysics = this.age > 2;

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime - 2) {
            this.remove();
            return;
        }

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= friction;
        this.yd *= friction;
        this.zd *= friction;

        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    public int texIndex(float partialTicks) {
        int clamped = Math.min(this.age, this.lifetime);
        return clamped * 25 / this.lifetime;
    }

    public Identifier texture() {
        return TEXTURE;
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

    @Override
    public ParticleRenderType getGroup() {
        return MUKE_CLOUD_GROUP;
    }
}
