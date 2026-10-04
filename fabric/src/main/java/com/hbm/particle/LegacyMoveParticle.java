// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public abstract class LegacyMoveParticle extends Particle {

    protected LegacyMoveParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected LegacyMoveParticle(
            ClientLevel level, double x, double y, double z, double xa, double ya, double za) {
        super(level, x, y, z, xa, ya, za);
    }

    @Override
    public void move(double xa, double ya, double za) {
        double originalXa = xa;
        double originalYa = ya;
        double originalZa = za;
        if (this.hasPhysics
                && (xa != 0.0 || ya != 0.0 || za != 0.0)
                && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            Vec3 movement =
                    Entity.collideBoundingBox(
                            CollisionContext.positionContext(this.y),
                            new Vec3(xa, ya, za),
                            this.getBoundingBox(),
                            this.level,
                            List.of());
            xa = movement.x;
            ya = movement.y;
            za = movement.z;
        }

        if (xa != 0.0 || ya != 0.0 || za != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
            this.setLocationFromBoundingbox();
        }

        this.onGround = originalYa != ya && originalYa < 0.0;
        if (originalXa != xa) this.xd = 0.0;
        if (originalYa != ya) this.yd = 0.0;
        if (originalZa != za) this.zd = 0.0;
    }
}
