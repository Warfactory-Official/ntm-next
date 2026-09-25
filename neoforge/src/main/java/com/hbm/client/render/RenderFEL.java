// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.machine.BlockEntityFEL;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.awt.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFEL
        implements BlockEntityRenderer<BlockEntityFEL, RenderFEL.State>,
                ConcurrentRenderStateExtraction {

    public RenderFEL(BlockEntityRendererProvider.Context context) {}

    @Override
    public AABB getRenderBoundingBox(BlockEntityFEL be) {
        BlockPos pos = be.getBlockPos();
        Direction facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        int reach = Mth.clamp(be.distance, 0, BlockEntityFEL.RANGE);
        return new AABB(pos).minmax(new AABB(pos.relative(facing, reach))).inflate(2.0D);
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
            BlockEntityFEL fel,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                fel, state, partialTicks, cameraPosition, breakProgress);
        state.facing = fel.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.length = fel.distance - 3;
        state.active =
                fel.power > BlockEntityFEL.powerReq * Math.pow(2, fel.mode.ordinal())
                        && fel.isOn
                        && fel.mode != EnumWavelengths.NULL
                        && state.length > 0;

        long time = fel.getLevel().getGameTime();
        state.color =
                fel.mode.renderedBeamColor == 0
                        ? Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 0xFFFFFF
                        : fel.mode.renderedBeamColor;
        state.start = (int) (time % 1000 / 2);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (!state.active) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 0)));
        poseStack.translate(0.0, 1.5, -1.5);

        Vec3 skeleton = new Vec3(0, 0, -state.length - 1);
        BeamPronter.prontBeam(
                poseStack,
                collector,
                skeleton,
                EnumWaveType.SPIRAL,
                EnumBeamType.SOLID,
                state.color,
                state.color,
                0,
                1,
                0F,
                2,
                0.0625F);
        BeamPronter.prontBeam(
                poseStack,
                collector,
                skeleton,
                EnumWaveType.RANDOM,
                EnumBeamType.SOLID,
                state.color,
                state.color,
                state.start,
                state.length / 2 + 1,
                0.0625F,
                2,
                0.0625F);

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean active;
        public int length;
        public int color;
        public int start;
    }
}
