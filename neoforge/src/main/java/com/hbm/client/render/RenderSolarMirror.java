// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntitySolarMirror;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSolarMirror
        implements BlockEntityRenderer<BlockEntitySolarMirror, RenderSolarMirror.State>,
                ConcurrentRenderStateExtraction {
    private static final int MIRROR = ResourceManager.solar_mirror.partId("Mirror");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderSolarMirror() {
        this.model = ResourceManager.solar_mirror;

        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.solar_mirror_tex);
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
    public AABB getRenderBoundingBox(BlockEntitySolarMirror be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 25,
                pos.getY() - 25,
                pos.getZ() - 25,
                pos.getX() + 25,
                pos.getY() + 25,
                pos.getZ() + 25);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntitySolarMirror be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        int dx = be.tX - be.getBlockPos().getX();
        int dy = be.tY - be.getBlockPos().getY();
        int dz = be.tZ - be.getBlockPos().getZ();

        state.aimed = be.tY > be.getBlockPos().getY();
        if (state.aimed) {
            double dist = Math.sqrt((double) dx * dx + dy * dy + dz * dz);

            state.pitch =
                    (float)
                            (Math.toDegrees(
                                            -Math.asin(
                                                    Math.max(
                                                            -1.0,
                                                            Math.min(1.0, (dy + 0.5) / dist))))
                                    + 90);
            state.yaw = (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180);
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.translate(0.0, 1.0, 0.0);
        if (s.aimed) {
            ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
            ps.mulPose(Axis.ZP.rotationDegrees(s.pitch));
        }
        ps.translate(0.0, -1.0, 0.0);
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, MIRROR));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean aimed;
        public float pitch;
        public float yaw;
    }
}
