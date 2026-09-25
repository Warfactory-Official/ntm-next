// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

public final class OrbitalLaserGeometry {

    public static final double STEP_RADIANS = Math.PI / 4D;
    public static final int VERTEX_COUNT = 2 * 8 * 4;
    public static final float[] XYZ = new float[VERTEX_COUNT * 3];
    public static final int[] ARGB = new int[VERTEX_COUNT];

    static {
        int vertex = 0;
        for (int pass = 0; pass < 2; pass++) {
            float scale = pass == 0 ? 1F : 0.5F;
            int color = pass == 0 ? 0xFFFF0000 : 0xFFFFFFFF;
            for (int i = 0; i < 8; i++) {
                double angle = i * STEP_RADIANS;
                double nextAngle = (i + 1) * STEP_RADIANS;
                double x = 0.5D * Math.cos(angle);
                double z = -0.5D * Math.sin(angle);
                double nextX = i == 7 ? 0.5D : 0.5D * Math.cos(nextAngle);
                double nextZ = i == 7 ? 0D : -0.5D * Math.sin(nextAngle);
                vertex = write(vertex, x * scale, 250, z * scale, color);
                vertex = write(vertex, x * scale, 0, z * scale, color);
                vertex = write(vertex, nextX * scale, 0, nextZ * scale, color);
                vertex = write(vertex, nextX * scale, 250, nextZ * scale, color);
            }
        }
    }

    private OrbitalLaserGeometry() {}

    private static int write(int at, double x, double y, double z, int color) {
        int start = at * 3;
        XYZ[start] = (float) x;
        XYZ[start + 1] = (float) y;
        XYZ[start + 2] = (float) z;
        ARGB[at] = color;
        return at + 1;
    }
}
