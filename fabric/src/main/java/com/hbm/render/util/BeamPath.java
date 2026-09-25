// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record BeamPath(Matrix4f frame, double[] x, double[] y, double[] z, double length) {
    public static BeamPath trace(
            Matrix4fc base, Vec3 skeleton, EnumWaveType wave, int start, int segments, float size) {
        assert segments > 0;
        float yaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180D / Math.PI);
        float horizontal = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float pitch = (float) (Math.atan2(skeleton.y, horizontal) * 180D / Math.PI);
        var frame =
                new Matrix4f(base)
                        .rotate(Axis.YP.rotationDegrees(180F))
                        .rotate(Axis.YP.rotationDegrees(yaw))
                        .rotate(Axis.XP.rotationDegrees(pitch - 90F));
        double length = skeleton.length(), step = length / segments;
        var random = new Random(start);
        double[] x = new double[segments + 1],
                y = new double[segments + 1],
                z = new double[segments + 1];
        for (int i = 0; i <= segments; i++) {
            Vec3 spinner = new Vec3(size, 0, 0);
            if (wave == EnumWaveType.SPIRAL) {
                spinner =
                        spinner.yRot((float) Math.PI * start / 180F)
                                .yRot((float) Math.PI * 45F / 180F * i);
            } else {
                spinner =
                        spinner.yRot((float) Math.PI * 2F * random.nextFloat())
                                .yRot((float) Math.PI * 2F * random.nextFloat());
            }
            x[i] = spinner.x;
            y[i] = step * i + spinner.y;
            z[i] = spinner.z;
        }
        return new BeamPath(frame, x, y, z, length);
    }
}
