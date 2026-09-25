// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.particle;

import net.minecraft.world.phys.Vec3;

final class ModFXMotion {

    private ModFXMotion() {}

    static Vec3 spawn(double motionX, double motionY, double motionZ) {

        double x = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double y = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double z = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;

        float speed = (float) (Math.random() + Math.random() + 1.0D) * 0.15F;
        float length = (float) Math.sqrt(x * x + y * y + z * z);

        x = x / length * speed * 0.4000000059604645D;
        y = y / length * speed * 0.4000000059604645D + 0.10000000149011612D;
        z = z / length * speed * 0.4000000059604645D;

        return new Vec3(
                x * 0.10000000149011612D + motionX,
                y * 0.10000000149011612D + motionY,
                z * 0.10000000149011612D + motionZ);
    }
}
