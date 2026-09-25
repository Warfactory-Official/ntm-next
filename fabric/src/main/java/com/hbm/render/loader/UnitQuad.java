// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class UnitQuad {
    private static final Map<Layout, UnitQuad> LAYOUTS = new ConcurrentHashMap<>();
    private static final float PARALLELOGRAM_EPSILON = 1.0E-4F;

    private final GroupObject group;

    private UnitQuad(Layout layout) {
        float[] corners = {0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F};
        float[] quads = new float[GroupObject.QUAD];
        for (int c = 0; c < 4; c++) {
            int at = c * GroupObject.STRIDE;
            quads[at] = corners[c * 2];
            quads[at + 1] = corners[c * 2 + 1];
            quads[at + 3] = layout.uv[c * 2];
            quads[at + 4] = layout.uv[c * 2 + 1];
            quads[at + 5] = layout.nx;
            quads[at + 6] = layout.ny;
            quads[at + 7] = layout.nz;
        }
        group = new GroupObject("unit_quad", quads, new float[] {0F, 0F, 1F});
    }

    public static UnitQuad of(float[] uv, float nx, float ny, float nz) {
        return LAYOUTS.computeIfAbsent(new Layout(uv.clone(), nx, ny, nz), UnitQuad::new);
    }

    public GroupObject group() {
        return group;
    }

    public static boolean matrix(
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            Matrix4f out) {
        float ex = bx - ax, ey = by - ay, ez = bz - az;
        float fx = dx - ax, fy = dy - ay, fz = dz - az;
        assert Math.abs(cx - (bx + fx)) <= PARALLELOGRAM_EPSILON
                        && Math.abs(cy - (by + fy)) <= PARALLELOGRAM_EPSILON
                        && Math.abs(cz - (bz + fz)) <= PARALLELOGRAM_EPSILON
                : "not a parallelogram";
        float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
        float lengthSquared = nx * nx + ny * ny + nz * nz;
        if (lengthSquared == 0F) return false;
        float inverse = (float) (1D / Math.sqrt(lengthSquared));
        out.set(
                ex,
                ey,
                ez,
                0F,
                fx,
                fy,
                fz,
                0F,
                nx * inverse,
                ny * inverse,
                nz * inverse,
                0F,
                ax,
                ay,
                az,
                1F);
        return true;
    }

    public void visit(
            Visitor visitor,
            Matrix4f scratch,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz) {
        if (matrix(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, scratch))
            visitor.accept(this, scratch);
    }

    public void emit(
            PoseStack.Pose pose,
            PoseStack.Pose scratch,
            VertexConsumer buffer,
            Matrix4fc matrix,
            int light,
            int color) {
        scratch.set(pose);
        scratch.pose().mul(matrix);
        group.emit(scratch, buffer, light, color, OverlayTexture.NO_OVERLAY, true);
    }

    @FunctionalInterface
    public interface Visitor {
        void accept(UnitQuad quad, Matrix4fc matrix);
    }

    private record Layout(float[] uv, float nx, float ny, float nz) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Layout layout
                    && Arrays.equals(uv, layout.uv)
                    && nx == layout.nx
                    && ny == layout.ny
                    && nz == layout.nz;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(uv), nx, ny, nz);
        }
    }
}
