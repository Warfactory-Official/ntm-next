// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntitySolarBoiler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSolarBoiler
        implements BlockEntityRenderer<BlockEntitySolarBoiler, RenderSolarBoiler.State>,
                ConcurrentRenderStateExtraction {

    private static final int BEAM_LIMIT = 250;

    public RenderSolarBoiler(BlockEntityRendererProvider.Context context) {}

    private static void emitBeam(PoseStack.Pose pose, VertexConsumer buf, float far) {
        final float near = 1.0625F, max = 0.01F, min = 0.005F;
        wall(
                buf, pose, 0.5F, near, 0.5F, 0.5F, near, -0.5F, 0.5F, far, -0.5F, 0.5F, far, 0.5F,
                max, min);
        wall(
                buf, pose, -0.5F, near, 0.5F, -0.5F, near, -0.5F, -0.5F, far, -0.5F, -0.5F, far,
                0.5F, max, min);
        wall(
                buf, pose, 0.5F, near, 0.5F, -0.5F, near, 0.5F, -0.5F, far, 0.5F, 0.5F, far, 0.5F,
                max, min);
        wall(
                buf, pose, 0.5F, near, -0.5F, -0.5F, near, -0.5F, -0.5F, far, -0.5F, 0.5F, far,
                -0.5F, max, min);
    }

    private static void wall(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float max,
            float min) {
        int near = ARGB.colorFromFloat(max, 1F, 1F, 1F);
        int far = ARGB.colorFromFloat(min, 1F, 1F, 1F);
        Vertices.emit(buf, pose, x0, y0, z0, near);
        Vertices.emit(buf, pose, x1, y1, z1, near);
        Vertices.emit(buf, pose, x2, y2, z2, far);
        Vertices.emit(buf, pose, x3, y3, z3, far);
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
            BlockEntitySolarBoiler be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.mirrors.clear();

        if (Minecraft.getInstance().options.graphicsPreset().get() == GraphicsPreset.FAST) return;
        BlockPos pos = be.getBlockPos();
        int count = 0;
        for (BlockPos mirror : be.secondary) {
            if (count++ >= BEAM_LIMIT) break;
            state.mirrors.add(mirror.subtract(pos));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.mirrors.isEmpty()) return;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        for (Vec3i rel : s.mirrors) {

            double dx = -rel.getX(), dy = -rel.getY(), dz = -rel.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 1e-4) continue;

            double pitch =
                    Math.toDegrees(-Math.asin(Math.max(-1.0, Math.min(1.0, (dy + 0.5) / dist))))
                            + 90;
            double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180;

            ps.pushPose();
            ps.translate(rel.getX(), rel.getY(), rel.getZ());
            ps.translate(0, 1, 0);
            ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
            ps.mulPose(Axis.ZP.rotationDegrees((float) pitch));
            ps.translate(0, -1, 0);
            final float far = (float) dist;
            col.submitCustomGeometry(
                    ps, BeamRenderTypes.ADDITIVE, (pose, buf) -> emitBeam(pose, buf, far));
            ps.popPose();
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final List<Vec3i> mirrors = new ArrayList<>();
    }
}
