// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.tileentity.bomb.BlockEntityCharge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderExplosiveCharge
        implements BlockEntityRenderer<BlockEntityCharge, RenderExplosiveCharge.State>,
                ConcurrentRenderStateExtraction {

    private final Font font;

    public RenderExplosiveCharge(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityCharge charge,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                charge, state, partialTicks, cameraPosition, breakProgress);
        state.timer = charge.getMinutes() + ":" + charge.getSeconds();
        state.facing = charge.getBlockState().getValue(BlockChargeBase.FACING);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        switch (state.facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            case UP -> {}
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZN.rotationDegrees(90));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YN.rotationDegrees(90));
                poseStack.mulPose(Axis.ZN.rotationDegrees(90));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.mulPose(Axis.ZN.rotationDegrees(90));
            }
            case EAST -> poseStack.mulPose(Axis.ZN.rotationDegrees(90));
        }

        poseStack.translate(-0.05, -0.185, 0.15);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        collector.submitText(
                poseStack,
                0,
                0,
                FormattedCharSequence.forward(state.timer, Style.EMPTY),
                false,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                0xFF00FF00,
                0,
                0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        private String timer = "00:00";
        private Direction facing = Direction.UP;
    }
}
