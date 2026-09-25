// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.network.BlockEntityPipeAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPipeAnchor
        implements BlockEntityRenderer<BlockEntityPipeAnchor, RenderPipeAnchor.State>,
                ConcurrentRenderStateExtraction {
    private static final int PIPE = ResourceManager.pipe_anchor.partId("Pipe");
    private static final int RING = ResourceManager.pipe_anchor.partId("Ring");

    private final HFRWavefrontObject model = ResourceManager.pipe_anchor;
    private final RenderType renderType =
            RenderTypes.entityCutoutCull(ResourceManager.pipe_anchor_tex);

    public static boolean isDominant(Vec3 first, Vec3 second) {
        if (first.x != second.x) return first.x < second.x;
        if (first.y != second.y) return first.y < second.y;
        if (first.z != second.z) return first.z < second.z;
        return false;
    }

    private static int lighten(int rgb, double factor) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r = (int) (r + (255 - r) * factor);
        g = (int) (g + (255 - g) * factor);
        b = (int) (b + (255 - b) * factor);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityPipeAnchor be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.runs.clear();
        if (be.fluid == Fluids.EMPTY && be.connections.length == 0) return;
        state.color = lighten(ARGB.transparent(FluidPipeTintData.colorFor(be.fluid)), 0.25);

        Vec3 self = Vec3.atCenterOf(be.getBlockPos());
        for (long key : be.connections) {
            BlockPos otherPos = BlockPos.of(key);
            if (!(be.getLevel() != null
                    && be.getLevel().getBlockEntity(otherPos)
                            instanceof BlockEntityPipeAnchor other)) continue;
            if (other.fluid != be.fluid) continue;
            Vec3 target = Vec3.atCenterOf(otherPos);
            if (!isDominant(self, target)) continue;

            double dX = target.x - self.x;
            double dY = target.y - self.y;
            double dZ = target.z - self.z;
            double hyp = Mth.length(dX, dZ);
            Run run = new Run();
            run.yaw = (float) Math.toDegrees(Math.atan2(dX, dZ));
            run.pitch = (float) Math.toDegrees(Math.atan2(dY, hyp));
            run.length = (float) Mth.length(dX, dY, dZ);
            state.runs.add(run);
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.runs.isEmpty()) return;
        int light = s.lightCoords;
        int pipeColor = ARGB.opaque(s.color);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        for (Run run : s.runs) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(run.yaw));
            ps.mulPose(Axis.XP.rotationDegrees(90F - run.pitch));

            ps.pushPose();
            ps.scale(1F, run.length, 1F);
            ps.translate(0.0, -0.5, 0.0);
            col.submitCustomGeometry(
                    ps,
                    renderType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, pipeColor, PIPE));
            ps.popPose();

            ps.pushPose();
            ps.translate(0.0, run.length / 2D - 1.5, 0.0);
            col.submitCustomGeometry(
                    ps,
                    renderType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, RING));
            ps.popPose();

            ps.popPose();
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityPipeAnchor be) {
        BlockPos pos = be.getBlockPos();
        AABB box = new AABB(pos);
        Vec3 self = Vec3.atCenterOf(pos);
        for (long key : be.connections) {
            BlockPos peer = BlockPos.of(key);

            if (isDominant(self, Vec3.atCenterOf(peer))) box = box.minmax(new AABB(peer));
        }
        return box.inflate(1.0D);
    }

    public static final class Run {
        public float yaw;
        public float pitch;
        public float length;
    }

    public static final class State extends BlockEntityRenderState {
        public final List<Run> runs = new ArrayList<>();
        public int color = 0xFFFFFF;
    }
}
