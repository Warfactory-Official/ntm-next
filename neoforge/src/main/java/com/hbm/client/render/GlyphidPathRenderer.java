// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.util.GameTime;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GlyphidPathRenderer {

    public static final RenderPipeline PATH_LINES_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_glyphid_path_lines")
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.ALWAYS_PASS, false)));
    private static final RenderType PATH_LINES =
            RenderType.create(
                    "ntm_glyphid_path_lines",
                    RenderSetup.builder(PATH_LINES_PIPELINE).createRenderSetup());

    private static final double RANGE_SQ = 16384D;
    private static final long STALE_AFTER_MS = 3000L;

    private static final float NODE_HALF = 0.15F;
    private static final float NEXT_HALF = 0.28F;
    private static final float TARGET_HALF = 0.45F;
    private static final float LINK_HALF = 0.04F;

    private static final Map<Integer, Entry> PATHS = new ConcurrentHashMap<>();

    private GlyphidPathRenderer() {}

    private record Entry(
            int[] nodes,
            float[] costs,
            int nextIndex,
            int targetX,
            int targetY,
            int targetZ,
            float minCost,
            float maxCost,
            long stamp) {}

    public static void accept(
            int entityId,
            int[] nodes,
            float[] costs,
            int nextIndex,
            int targetX,
            int targetY,
            int targetZ) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float cost : costs) {
            if (cost < min) min = cost;
            if (cost > max) max = cost;
        }
        PATHS.put(
                entityId,
                new Entry(
                        nodes,
                        costs,
                        nextIndex,
                        targetX,
                        targetY,
                        targetZ,
                        min,
                        max,
                        GameTime.millis()));
    }

    public static void clear(int entityId) {
        PATHS.remove(entityId);
    }

    public static void clearAll() {
        PATHS.clear();
    }

    public static void submit(
            PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState state) {
        if (PATHS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PATHS.clear();
            return;
        }

        Vec3 camera = state.cameraRenderState.pos;
        long now = GameTime.now();

        PATHS.entrySet()
                .removeIf(
                        e ->
                                now - e.getValue().stamp() > STALE_AFTER_MS
                                        || mc.level.getEntity(e.getKey()) == null);

        for (Entry entry : PATHS.values()) {
            submitPath(poseStack, collector, entry, camera);
        }
    }

    private static void submitPath(
            PoseStack poseStack, SubmitNodeCollector collector, Entry entry, Vec3 camera) {
        int count = entry.costs().length;
        if (count == 0) return;

        double firstX = entry.nodes()[0] + 0.5D;
        double firstZ = entry.nodes()[2] + 0.5D;
        double dx = firstX - camera.x;
        double dz = firstZ - camera.z;
        if (dx * dx + dz * dz > RANGE_SQ) return;

        for (int i = 0; i < count; i++) {
            double x = entry.nodes()[i * 3] + 0.5D;
            double y = entry.nodes()[i * 3 + 1] + 0.5D;
            double z = entry.nodes()[i * 3 + 2] + 0.5D;

            boolean isNext = i == entry.nextIndex();
            float half = isNext ? NEXT_HALF : NODE_HALF;
            int color =
                    isNext
                            ? 0xFFFFFF
                            : costColor(entry.costs()[i], entry.minCost(), entry.maxCost());

            submitBox(poseStack, collector, camera, x, y, z, half, half, half, ARGB.opaque(color));

            if (i + 1 < count) {
                double nx = entry.nodes()[(i + 1) * 3] + 0.5D;
                double ny = entry.nodes()[(i + 1) * 3 + 1] + 0.5D;
                double nz = entry.nodes()[(i + 1) * 3 + 2] + 0.5D;
                submitLink(poseStack, collector, camera, x, y, z, nx, ny, nz, ARGB.opaque(color));
            }
        }

        submitBox(
                poseStack,
                collector,
                camera,
                entry.targetX() + 0.5D,
                entry.targetY() + 0.5D,
                entry.targetZ() + 0.5D,
                TARGET_HALF,
                TARGET_HALF,
                TARGET_HALF,
                ARGB.opaque(0x00FFFF));
    }

    private static void submitLink(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 camera,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            int color) {
        double cx = (x0 + x1) * 0.5D;
        double cy = (y0 + y1) * 0.5D;
        double cz = (z0 + z1) * 0.5D;
        float hx = (float) Math.max(Math.abs(x1 - x0) * 0.5D, LINK_HALF);
        float hy = (float) Math.max(Math.abs(y1 - y0) * 0.5D, LINK_HALF);
        float hz = (float) Math.max(Math.abs(z1 - z0) * 0.5D, LINK_HALF);
        submitBox(poseStack, collector, camera, cx, cy, cz, hx, hy, hz, color);
    }

    private static void submitBox(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 camera,
            double x,
            double y,
            double z,
            float hx,
            float hy,
            float hz,
            int color) {
        VoxelShape shape = Shapes.create(new AABB(-hx, -hy, -hz, hx, hy, hz));
        poseStack.pushPose();
        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
        collector.submitShapeOutline(poseStack, shape, PATH_LINES, color, 1.0F, false);
        poseStack.popPose();
    }

    private static int costColor(float cost, float min, float max) {
        float t = max - min <= 1.0E-4F ? 0F : Mth.clamp((cost - min) / (max - min), 0F, 1F);
        int r = (int) (t * 255F);
        int b = 255 - r;
        return (r << 16) | (64 << 8) | b;
    }
}
