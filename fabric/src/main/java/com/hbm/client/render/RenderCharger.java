// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCharger;
import com.hbm.util.Facing;
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
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCharger
        implements BlockEntityRenderer<BlockEntityCharger, RenderCharger.State>,
                ConcurrentRenderStateExtraction {

    private static final int LIGHT = ResourceManager.charger.partId("Light");
    private static final int SLIDE = ResourceManager.charger.partId("Slide");
    private static final int LEFT = ResourceManager.charger.partId("Left");
    private static final int RIGHT = ResourceManager.charger.partId("Right");

    private static final float TILT = 10F;
    private static final double PIVOT_X = -0.34375D;
    private static final double PIVOT_Y = 0.25D;

    private static final double DROP = 0.25D;

    private static final float SWIVEL = 30F;
    private static final double HINGE_Y = 0.28D;

    private static final int LAMP = 0xFFFFBF00;

    private final HFRWavefrontObject model = ResourceManager.charger;
    private final RenderType bodyType = RenderTypes.entityCutoutCull(ResourceManager.charger_tex);

    private final RenderType litType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.charger_tex);
    private final RenderType lampType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.white_tex);

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityCharger be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(HorizontalDirectionalBlock.FACING));

        double time =
                Mth.lerp(partialTicks, be.lastUsingTicks, be.usingTicks)
                        / (double) BlockEntityCharger.DELAY;
        state.extend = (float) Math.min(1D, time * 2D);
        state.swivel = (float) Math.max(0D, (time - 0.5D) * 2D);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        col.submitCustomGeometry(
                ps,
                lampType,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, LAMP, LIGHT));

        ps.pushPose();
        ps.translate(PIVOT_X, PIVOT_Y, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(TILT));
        ps.translate(-PIVOT_X, -PIVOT_Y, 0);
        ps.translate(0, -DROP * s.extend, 0);

        int light = s.lightCoords;
        col.submitCustomGeometry(
                ps,
                litType,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, SLIDE));
        submitClaw(ps, col, light, s.swivel * SWIVEL, LEFT);
        submitClaw(ps, col, light, s.swivel * -SWIVEL, RIGHT);
        ps.popPose();

        ps.popPose();
    }

    private void submitClaw(
            PoseStack ps, SubmitNodeCollector col, int light, float degrees, int part) {
        ps.pushPose();
        ps.translate(0, HINGE_Y, 0);
        ps.mulPose(Axis.XP.rotationDegrees(degrees));
        ps.translate(0, -HINGE_Y, 0);
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, part));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float extend;
        public float swivel;
    }
}
