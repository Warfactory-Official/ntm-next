// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKCraneConsole
        implements BlockEntityRenderer<BlockEntityCraneConsole, RenderRBMKCraneConsole.State>,
                ConcurrentRenderStateExtraction {
    private static final int JOYSTICK = ResourceManager.rbmk_crane_console.partId("Joystick");
    private static final int METER1 = ResourceManager.rbmk_crane_console.partId("Meter1");
    private static final int METER2 = ResourceManager.rbmk_crane_console.partId("Meter2");
    private static final int LAMP1 = ResourceManager.rbmk_crane_console.partId("Lamp1");
    private static final int LAMP2 = ResourceManager.rbmk_crane_console.partId("Lamp2");

    private static final int LOADING = ARGB.colorFromFloat(1F, 0.8F, 0.8F, 0F);
    private static final int LOADED = ARGB.colorFromFloat(1F, 0F, 1F, 0F);
    private static final int UNLOADED = ARGB.colorFromFloat(1F, 0F, 0.1F, 0F);
    private static final int VALID = ARGB.colorFromFloat(1F, 0F, 1F, 0F);
    private static final int INVALID = ARGB.colorFromFloat(1F, 1F, 0F, 0F);
    private static final int GIRDER = ResourceManager.rbmk_crane.partId("Girder");
    private static final int MAIN = ResourceManager.rbmk_crane.partId("Main");
    private static final int TUBE = ResourceManager.rbmk_crane.partId("Tube");
    private static final int CARRIAGE = ResourceManager.rbmk_crane.partId("Carriage");
    private static final int LIFT = ResourceManager.rbmk_crane.partId("Lift");

    private final RenderType consoleType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.rbmk_crane_console_tex);
    private final RenderType craneType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.rbmk_crane_tex);

    private final RenderType lampType = FlatCutout.of(ResourceManager.white_tex);

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
    public AABB getRenderBoundingBox(BlockEntityCraneConsole be) {
        return AABB.INFINITE;
    }

    @Override
    public void extractRenderState(
            BlockEntityCraneConsole be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        Direction dir = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.facing = Facing.yaw(dir, 90);
        state.tiltFront = (float) Mth.lerp(partialTicks, be.lastTiltFront, be.tiltFront);
        state.tiltLeft = (float) Mth.lerp(partialTicks, be.lastTiltLeft, be.tiltLeft);
        state.loadedHeat = be.loadedHeat;
        state.loadedEnrichment = be.loadedEnrichment;
        state.lamp1 = be.isCraneLoading() ? LOADING : be.hasItemLoaded() ? LOADED : UNLOADED;
        state.lamp2 = be.isAboveValidTarget() ? VALID : INVALID;

        state.setUpCrane = be.setUpCrane;
        if (be.setUpCrane) {
            BlockPos p = be.getBlockPos();
            state.height = be.height - 6;
            state.cranePosX = be.centerX - p.getX();
            state.cranePosY = (be.centerY - p.getY()) + 1;
            state.cranePosZ = be.centerZ - p.getZ();
            state.posFront = Mth.lerp(partialTicks, be.lastPosFront, be.posFront);
            state.posLeft = Mth.lerp(partialTicks, be.lastPosLeft, be.posLeft);
            state.rotationOffset = be.craneRotationOffset;
            state.spanF = be.spanF;
            state.spanB = be.spanB;
            state.spanL = be.spanL;
            state.spanR = be.spanR;
            state.progress = Mth.lerp(partialTicks, be.lastProgress, be.progress);
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        final int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facing));
        poseStack.translate(0.5, 0.0, 0.0);

        poseStack.pushPose();
        poseStack.translate(0.75, 1.0, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.tiltFront));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.tiltLeft));
        poseStack.translate(-0.75, -1.015, 0.0);
        collector.submitCustomGeometry(
                poseStack,
                consoleType,
                (pose, buffer) ->
                        ResourceManager.rbmk_crane_console.renderPart(
                                pose, buffer, light, -1, JOYSTICK));
        poseStack.popPose();

        double jitter = Math.sin(GameTime.now() * 0.01 % 360) * 180 / Math.PI * 0.05;
        renderMeter(
                poseStack, collector, light, 0.75, jitter + 135 - 270 * state.loadedHeat, METER1);
        renderMeter(
                poseStack,
                collector,
                light,
                0.25,
                jitter + 135 - 270 * state.loadedEnrichment,
                METER2);
        int lamp1 = state.lamp1, lamp2 = state.lamp2;
        collector.submitCustomGeometry(
                poseStack,
                lampType,
                (pose, buffer) ->
                        ResourceManager.rbmk_crane_console.renderPart(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, lamp1, LAMP1));
        collector.submitCustomGeometry(
                poseStack,
                lampType,
                (pose, buffer) ->
                        ResourceManager.rbmk_crane_console.renderPart(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, lamp2, LAMP2));
        poseStack.popPose();

        if (state.setUpCrane) {
            poseStack.pushPose();
            poseStack.translate(0.5, -1.0, 0.5);
            poseStack.translate(state.cranePosX, state.cranePosY, state.cranePosZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.facing));
            poseStack.translate(-state.posFront, 0.0, state.posLeft);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.rotationOffset));

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.rotationOffset));
            int girderSpan;
            switch (state.rotationOffset) {
                case 90 -> {
                    girderSpan = state.spanL + state.spanR + 1;
                    poseStack.translate(0, 0, -state.posLeft - state.spanR);
                }
                case 180 -> {
                    girderSpan = state.spanF + state.spanB + 1;
                    poseStack.translate(state.posFront - state.spanF, 0, 0);
                }
                case 270 -> {
                    girderSpan = state.spanL + state.spanR + 1;
                    poseStack.translate(0, 0, -state.posLeft + state.spanL);
                }
                default -> {
                    girderSpan = state.spanF + state.spanB + 1;
                    poseStack.translate(state.posFront + state.spanB, 0, 0);
                }
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(state.rotationOffset));
            for (int i = 0; i < girderSpan; i++) {
                collector.submitCustomGeometry(
                        poseStack,
                        craneType,
                        (pose, buffer) ->
                                ResourceManager.rbmk_crane.renderPart(
                                        pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, GIRDER));
                poseStack.translate(-1.0, 0.0, 0.0);
            }
            poseStack.popPose();

            collector.submitCustomGeometry(
                    poseStack,
                    craneType,
                    (pose, buffer) ->
                            ResourceManager.rbmk_crane.renderPart(
                                    pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, MAIN));

            poseStack.pushPose();
            for (int i = 0; i < state.height; i++) {
                collector.submitCustomGeometry(
                        poseStack,
                        craneType,
                        (pose, buffer) ->
                                ResourceManager.rbmk_crane.renderPart(
                                        pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, TUBE));
                poseStack.translate(0.0, 1.0, 0.0);
            }
            poseStack.translate(0.0, -1.0, 0.0);
            collector.submitCustomGeometry(
                    poseStack,
                    craneType,
                    (pose, buffer) ->
                            ResourceManager.rbmk_crane.renderPart(
                                    pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, CARRIAGE));
            poseStack.popPose();

            poseStack.translate(0.0, -3.25 * (1 - state.progress), 0.0);
            collector.submitCustomGeometry(
                    poseStack,
                    craneType,
                    (pose, buffer) ->
                            ResourceManager.rbmk_crane.renderPart(
                                    pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, LIFT));

            poseStack.popPose();
        }
    }

    private void renderMeter(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            double z,
            double angle,
            int part) {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.25, z);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) angle));
        poseStack.translate(0.0, -1.25, -z);
        collector.submitCustomGeometry(
                poseStack,
                consoleType,
                (pose, buffer) ->
                        ResourceManager.rbmk_crane_console.renderPart(
                                pose, buffer, light, -1, part));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float facing;
        public float tiltFront, tiltLeft;
        public double loadedHeat, loadedEnrichment;
        public int lamp1, lamp2;

        public boolean setUpCrane;
        public int height;
        public double cranePosX, cranePosY, cranePosZ;
        public double posFront, posLeft;
        public int rotationOffset;
        public int spanF, spanB, spanL, spanR;
        public double progress;
    }
}
