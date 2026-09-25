// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.animloader;

import com.hbm.util.BobMathUtil;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Transform {
    private final Vector3f scale;
    private final Vector3f translation;
    private final Quaternionf rotation;

    boolean hidden;

    public Transform(float[] matrix) {

        float[] normalized = matrix.clone();
        scale = extractScale(normalized);
        rotation = new Quaternionf().setFromNormalized(matrixFromArray(normalized));
        translation = new Vector3f(matrix[12], matrix[13], matrix[14]);
    }

    private static Vector3f extractScale(float[] matrix) {
        float scaleX = length(matrix[0], matrix[1], matrix[2]);
        float scaleY = length(matrix[4], matrix[5], matrix[6]);
        float scaleZ = length(matrix[8], matrix[9], matrix[10]);

        matrix[0] /= scaleX;
        matrix[1] /= scaleX;
        matrix[2] /= scaleX;

        matrix[4] /= scaleY;
        matrix[5] /= scaleY;
        matrix[6] /= scaleY;

        matrix[8] /= scaleZ;
        matrix[9] /= scaleZ;
        matrix[10] /= scaleZ;

        return new Vector3f(scaleX, scaleY, scaleZ);
    }

    private static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    static Matrix4f matrixFromArray(float[] matrix) {
        return new Matrix4f(
                matrix[0],
                matrix[1],
                matrix[2],
                matrix[3],
                matrix[4],
                matrix[5],
                matrix[6],
                matrix[7],
                matrix[8],
                matrix[9],
                matrix[10],
                matrix[11],
                matrix[12],
                matrix[13],
                matrix[14],
                matrix[15]);
    }

    public Matrix4f interpolate(
            Matrix4f dest, Transform other, float inter, Quaternionf rotationScratch) {
        float tx = (float) BobMathUtil.interp(translation.x, other.translation.x, inter);
        float ty = (float) BobMathUtil.interp(translation.y, other.translation.y, inter);
        float tz = (float) BobMathUtil.interp(translation.z, other.translation.z, inter);

        float sx = (float) BobMathUtil.interp(scale.x, other.scale.x, inter);
        float sy = (float) BobMathUtil.interp(scale.y, other.scale.y, inter);
        float sz = (float) BobMathUtil.interp(scale.z, other.scale.z, inter);

        Quaternionf quat = rotation.slerp(other.rotation, inter, rotationScratch);
        return dest.translationRotateScale(tx, ty, tz, quat.x, quat.y, quat.z, quat.w, sx, sy, sz);
    }
}
