// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityForceField;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderMachineForceField
        implements BlockEntityRenderer<BlockEntityForceField, RenderMachineForceField.State>,
                ConcurrentRenderStateExtraction {

    static final RenderPipeline SHELL_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_forcefield_shell")
                            .withColorTargetState(ColorTargetState.DEFAULT));

    private static final RenderType SHELL =
            RenderType.create(
                    "ntm_forcefield_shell",
                    RenderSetup.builder(SHELL_PIPELINE).createRenderSetup());

    private static final float LINE_WIDTH = 1F;

    private static int segments(float radius) {
        return (int) (16 + radius * 0.125);
    }

    private static void emitShell(
            PoseStack.Pose pose, VertexConsumer buf, int l, int s, float rad, int color) {
        float sRot = 360F / s;
        float lRot = (float) Math.PI / l;

        double[] vec = new double[3];
        for (int k = 0; k < s; k++) {
            double yaw = Math.toRadians((k + 1) * (double) sRot);
            double cos = Math.cos(yaw);
            double sin = Math.sin(yaw);

            vec[0] = 0;
            vec[1] = rad;
            vec[2] = 0;
            for (int i = 0; i < l; i++) {
                double px = vec[0], py = vec[1], pz = vec[2];
                rotateAroundX(vec, lRot);
                segment(
                        pose,
                        buf,
                        color,
                        px * cos + pz * sin,
                        py,
                        pz * cos - px * sin,
                        vec[0] * cos + vec[2] * sin,
                        vec[1],
                        vec[2] * cos - vec[0] * sin);
            }
        }

        float sRot2 = (float) Math.PI * 2F / (float) s;
        vec[0] = 0;
        vec[1] = rad;
        vec[2] = 0;
        for (int k = 0; k < l; k++) {
            rotateAroundZ(vec, lRot);
            for (int i = 0; i < s; i++) {
                double px = vec[0], py = vec[1], pz = vec[2];
                rotateAroundY(vec, sRot2);
                segment(pose, buf, color, px, py, pz, vec[0], vec[1], vec[2]);
            }
        }
    }

    private static void rotateAroundX(double[] v, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double y = v[1] * cos + v[2] * sin;
        double z = v[2] * cos - v[1] * sin;
        v[1] = y;
        v[2] = z;
    }

    private static void rotateAroundY(double[] v, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double x = v[0] * cos + v[2] * sin;
        double z = v[2] * cos - v[0] * sin;
        v[0] = x;
        v[2] = z;
    }

    private static void rotateAroundZ(double[] v, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double x = v[0] * cos + v[1] * sin;
        double y = v[1] * cos - v[0] * sin;
        v[0] = x;
        v[1] = y;
    }

    private static void segment(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int color,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2) {
        float nx = (float) (x2 - x1), ny = (float) (y2 - y1), nz = (float) (z2 - z1);
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0F) return;
        nx /= len;
        ny /= len;
        nz /= len;
        Vertices.emitLine(
                buf, pose, (float) x1, (float) y1, (float) z1, color, nx, ny, nz, LINE_WIDTH);
        Vertices.emitLine(
                buf, pose, (float) x2, (float) y2, (float) z2, color, nx, ny, nz, LINE_WIDTH);
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityForceField be) {
        return new AABB(be.getBlockPos()).inflate(be.radius + 1.0D);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityForceField be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.radius = be.radius;
        state.color = ARGB.opaque(be.color);
        state.up = be.isOn && be.health > 0 && be.power > 0 && be.cooldown == 0;

        state.spin = (float) ((GameTime.now() / 10.0D) % 360.0D);
    }

    @Override
    public void submit(
            State state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;

        ps.pushPose();

        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180F));
        ps.translate(0.0D, 0.5D, 0.0D);

        if (state.up) {
            int l = segments(state.radius);
            int s = l * 2;
            float rad = state.radius;
            int color = state.color;
            collector.submitCustomGeometry(
                    ps, SHELL, (pose, buf) -> emitShell(pose, buf, l, s, rad, color));
            ps.mulPose(Axis.YP.rotationDegrees(-state.spin));
        }

        ps.translate(0.0D, 0.5D, 0.0D);
        collector.submitCustomGeometry(
                ps,
                Sheets.TORUS,
                (pose, buf) -> ResourceManager.forcefield_top.render(pose, buf, light, -1));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float radius;
        public int color;
        public boolean up;
        public float spin;
    }

    private static final class Sheets {
        static final RenderType TORUS =
                WorldRenderPipeline.oneSidedCutout(ResourceManager.forcefield_top_tex);
    }
}
