// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.DemonLamp;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityDemonLamp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderDemonLamp
        implements BlockEntityRenderer<BlockEntityDemonLamp, RenderDemonLamp.State>,
                ConcurrentRenderStateExtraction {

    public static final int RAYS = 16;
    public static final double NEAR = 0.375D;
    public static final double FAR = 15D;
    public static final int INNER = ARGB.colorFromFloat(0.25F, 0F, 0.75F, 1F);
    public static final int OUTER = ARGB.colorFromFloat(0F, 0F, 0.75F, 1F);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityDemonLamp lamp,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                lamp, state, partialTicks, cameraPosition, breakProgress);
        state.facing = lamp.getBlockState().getValue(DemonLamp.FACING);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        orient(poseStack, state.facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        collector.submitCustomGeometry(
                poseStack, BeamRenderTypes.ADDITIVE, RenderDemonLamp::emitRays);
        poseStack.popPose();
    }

    public static void orient(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case UP -> {}
            case NORTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(270));
            }
        }
    }

    public static void emitRays(PoseStack.Pose pose, VertexConsumer buf) {
        float turn = (float) (Math.PI * 2D / RAYS);
        float cos = Mth.cos(turn);
        float sin = Mth.sin(turn);
        double x = 1D, z = 0D;
        for (int j = 0; j < 2; j++) {
            float y0 = (float) (0.5D + j * 0.125D);
            float y1 = (float) (0.5D + j * 0.125D + (j == 0 ? -0.5D : 0.5D));
            for (int i = 0; i < RAYS; i++) {
                Vertices.emit(buf, pose, (float) (x * NEAR), y0, (float) (z * NEAR), INNER);
                Vertices.emit(buf, pose, (float) (x * FAR), y1, (float) (z * FAR), OUTER);
                double nx = x * cos + z * sin;
                z = z * cos - x * sin;
                x = nx;
                Vertices.emit(buf, pose, (float) (x * FAR), y1, (float) (z * FAR), OUTER);
                Vertices.emit(buf, pose, (float) (x * NEAR), y0, (float) (z * NEAR), INNER);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.DOWN;
    }
}
