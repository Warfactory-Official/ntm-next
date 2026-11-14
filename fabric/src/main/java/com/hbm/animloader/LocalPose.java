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

package com.hbm.animloader;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class LocalPose {
    private final float[] values;
    private final boolean hidden;

    public LocalPose(float[] columnMajor, boolean hidden) {
        if (columnMajor.length != 16) {
            throw new IllegalArgumentException("A local pose requires sixteen matrix components");
        }
        for (float value : columnMajor) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Nonfinite local pose");
            }
        }
        if (columnMajor[3] != 0
                || columnMajor[7] != 0
                || columnMajor[11] != 0
                || columnMajor[15] != 1) {
            throw new IllegalArgumentException("A local pose must be affine");
        }
        float sx = length(columnMajor[0], columnMajor[1], columnMajor[2]);
        float sy = length(columnMajor[4], columnMajor[5], columnMajor[6]);
        float sz = length(columnMajor[8], columnMajor[9], columnMajor[10]);
        if (!(sx > 0 && sy > 0 && sz > 0)) {
            throw new IllegalArgumentException("A local pose requires nonzero scale");
        }
        var rotation =
                new Matrix3f(
                        columnMajor[0] / sx,
                        columnMajor[1] / sx,
                        columnMajor[2] / sx,
                        columnMajor[4] / sy,
                        columnMajor[5] / sy,
                        columnMajor[6] / sy,
                        columnMajor[8] / sz,
                        columnMajor[9] / sz,
                        columnMajor[10] / sz);
        float xy =
                rotation.m00() * rotation.m10()
                        + rotation.m01() * rotation.m11()
                        + rotation.m02() * rotation.m12();
        float xz =
                rotation.m00() * rotation.m20()
                        + rotation.m01() * rotation.m21()
                        + rotation.m02() * rotation.m22();
        float yz =
                rotation.m10() * rotation.m20()
                        + rotation.m11() * rotation.m21()
                        + rotation.m12() * rotation.m22();
        if (Math.abs(xy) > 0.0001f
                || Math.abs(xz) > 0.0001f
                || Math.abs(yz) > 0.0001f
                || Math.abs(rotation.determinant() - 1) > 0.0001f) {
            throw new IllegalArgumentException(
                    "A sampled local pose requires a proper rotation without shear");
        }
        var orientation = new Quaternionf().setFromNormalized(rotation).normalize();
        values =
                new float[] {
                    columnMajor[12],
                    columnMajor[13],
                    columnMajor[14],
                    orientation.x,
                    orientation.y,
                    orientation.z,
                    orientation.w,
                    sx,
                    sy,
                    sz
                };
        this.hidden = hidden;
    }

    private LocalPose(LocalPose pose, boolean hidden) {
        this.values = pose.values;
        this.hidden = hidden;
    }

    public LocalPose withHidden(boolean hidden) {
        return new LocalPose(this, hidden);
    }

    public float getComponent(int component) {
        return values[component];
    }

    public boolean isHidden() {
        return hidden;
    }

    public void interpolate(LocalPose next, float fraction, Matrix4f destination) {
        interpolate(values, 0, next.values, 0, fraction, destination);
    }

    static void interpolate(
            float[] first,
            int firstOffset,
            float[] second,
            int secondOffset,
            float fraction,
            Matrix4f destination) {
        float x = first[firstOffset + 3];
        float y = first[firstOffset + 4];
        float z = first[firstOffset + 5];
        float w = first[firstOffset + 6];
        float nx = second[secondOffset + 3];
        float ny = second[secondOffset + 4];
        float nz = second[secondOffset + 5];
        float nw = second[secondOffset + 6];
        float dot = x * nx + y * ny + z * nz + w * nw;
        if (dot < 0) {
            dot = -dot;
            nx = -nx;
            ny = -ny;
            nz = -nz;
            nw = -nw;
        }
        float firstWeight;
        float secondWeight;
        if (dot > 0.9999999f) {
            firstWeight = 1 - fraction;
            secondWeight = fraction;
        } else {
            double angle = Math.acos(Math.min(dot, 1));
            double divisor = Math.sin(angle);
            firstWeight = (float) (Math.sin((1 - fraction) * angle) / divisor);
            secondWeight = (float) (Math.sin(fraction * angle) / divisor);
        }
        x = firstWeight * x + secondWeight * nx;
        y = firstWeight * y + secondWeight * ny;
        z = firstWeight * z + secondWeight * nz;
        w = firstWeight * w + secondWeight * nw;
        float inverseLength = 1 / length(x, y, z, w);
        destination.translationRotateScale(
                mix(first[firstOffset], second[secondOffset], fraction),
                mix(first[firstOffset + 1], second[secondOffset + 1], fraction),
                mix(first[firstOffset + 2], second[secondOffset + 2], fraction),
                x * inverseLength,
                y * inverseLength,
                z * inverseLength,
                w * inverseLength,
                mix(first[firstOffset + 7], second[secondOffset + 7], fraction),
                mix(first[firstOffset + 8], second[secondOffset + 8], fraction),
                mix(first[firstOffset + 9], second[secondOffset + 9], fraction));
    }

    private static float mix(float first, float second, float fraction) {
        return first + (second - first) * fraction;
    }

    private static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private static float length(float x, float y, float z, float w) {
        return (float) Math.sqrt(x * x + y * y + z * z + w * w);
    }
}
