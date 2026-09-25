// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.client.model.FanModel;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityFan;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFan
        implements BlockEntityRenderer<BlockEntityFan, RenderFan.State>,
                ConcurrentRenderStateExtraction {
    private static final int BLADES = ResourceManager.fan.partId(FanModel.BLADES);

    private static final RenderType FAN = RenderTypes.entityCutoutCull(ResourceManager.fan_tex);

    private static void rotate(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180F));
            case UP -> {}
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90F));
        }
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
            BlockEntityFan be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(MachineFan.FACING);
        state.spin = be.prevSpin + (be.spin - be.prevSpin) * partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0.5D, 0.5D, 0.5D);
        rotate(poseStack, state.facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.spin));
        collector.submitCustomGeometry(
                poseStack,
                FAN,
                (pose, buffer) ->
                        ResourceManager.fan.renderPart(
                                pose, buffer, state.lightCoords, -1, BLADES));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float spin;
    }
}
