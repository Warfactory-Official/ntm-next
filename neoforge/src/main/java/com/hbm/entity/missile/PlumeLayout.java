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

package com.hbm.entity.missile;

import net.minecraft.util.Mth;

@FunctionalInterface
public interface PlumeLayout {

    PlumeLayout SINGLE =
            (_, _, _, out) -> {
                out[0] = out[1] = out[2] = 0D;
                return 1;
            };

    PlumeLayout CROSS =
            (yaw, pitch, _, out) -> {
                tilt(0D, 0D, 0.5D, yaw, pitch, out);
                double qx = out[0], qy = out[1], qz = out[2];
                set(out, 0, qx, qy, qz);
                set(out, 1, -qz, qy, qx);
                set(out, 2, -qx, -qz, -qz);
                set(out, 3, qz, -qz, -qx);
                return 4;
            };

    PlumeLayout TRIO =
            (yaw, pitch, facing, out) -> {
                float turn =
                        switch (facing) {
                            case 2 -> -Mth.HALF_PI;
                            case 4 -> -Mth.PI;
                            case 3 -> -3F * Mth.HALF_PI;
                            default -> 0F;
                        };
                tilt(Mth.sin(turn), 0D, Mth.cos(turn), yaw, pitch, out);
                double qx = out[0], qy = out[1], qz = out[2];
                set(out, 0, qx, qy, qz);
                set(out, 1, 0D, 0D, 0D);
                set(out, 2, -qx, -qz, -qz);
                return 3;
            };

    int offsets(float yaw, float pitch, byte facing, double[] out);

    private static void tilt(double x, double y, double z, float yaw, float pitch, double[] out) {
        float turn = (yaw + 90F) * (float) Math.PI / 180F;
        float tip = pitch * (float) Math.PI / 180F;
        float cos = Mth.cos(turn), sin = Mth.sin(turn);
        double x1 = x * cos + z * sin, z1 = z * cos - x * sin;
        cos = Mth.cos(tip);
        sin = Mth.sin(tip);
        double y2 = y * cos + z1 * sin, z2 = z1 * cos - y * sin;
        cos = Mth.cos(-turn);
        sin = Mth.sin(-turn);
        out[0] = x1 * cos + z2 * sin;
        out[1] = y2;
        out[2] = z2 * cos - x1 * sin;
    }

    private static void set(double[] out, int plume, double x, double y, double z) {
        out[3 * plume] = x;
        out[3 * plume + 1] = y;
        out[3 * plume + 2] = z;
    }
}
