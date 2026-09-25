// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.util.Facing;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKConsole
        implements BlockEntityRenderer<BlockEntityRBMKConsole, RenderRBMKConsole.State>,
                ConcurrentRenderStateExtraction {
    private final Font font;

    public RenderRBMKConsole(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKConsole be) {
        BlockPos p = be.getBlockPos();
        return new AABB(
                p.getX() - 2, p.getY(), p.getZ() - 2, p.getX() + 3, p.getY() + 4, p.getZ() + 3);
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
            BlockEntityRBMKConsole be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        Direction dir = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.yaw = Facing.yaw(dir, 90);
        state.columns = be.columns.clone();
        for (int i = 0; i < 6; i++) {
            Component text = screenText(be.screens[i].display);
            state.screenText[i] = text == null ? null : text.getVisualOrderText();
        }
    }

    public static @Nullable Component screenText(@Nullable String display) {
        if (display == null || display.isEmpty()) return null;
        String[] parts = display.split("=");
        return parts.length == 2
                ? Component.translatable(parts[0], parts[1])
                : Component.literal(display);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0.5, 0.0, 0.0);

        RBMKColumn[] columns = state.columns;
        collector.submitCustomGeometry(
                poseStack,
                RBMKColumnGrid.PANEL,
                (pose, buffer) ->
                        RBMKColumnGrid.emit(
                                pose, buffer, columns, 15, -0.3725F, 3.625F, 0.125F * 7F));
        emitScreens(state, poseStack, collector);

        poseStack.popPose();
    }

    private void emitScreens(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.translate(-0.42, 3.5, 1.75);
        for (int i = 0; i < 6; i++) {
            FormattedCharSequence text = state.screenText[i];
            poseStack.pushPose();
            if (i % 2 == 1) poseStack.translate(0.0, 0.0, 1.75 * -2);
            poseStack.translate(0.0, -0.75 * (i >> 1), 0.0);
            if (text != null) {
                int width = font.width(text);
                float f3 = Math.min(0.03F, 0.8F / Math.max(width, 1));
                poseStack.scale(f3, -f3, f3);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                collector.submitText(
                        poseStack,
                        -width / 2,
                        -font.lineHeight / 2,
                        text,
                        false,
                        Font.DisplayMode.POLYGON_OFFSET,
                        LightCoordsUtil.FULL_BRIGHT,
                        CommonColors.GREEN,
                        0,
                        0);
            }
            poseStack.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        public final FormattedCharSequence[] screenText = new FormattedCharSequence[6];
        public float yaw;
        public RBMKColumn[] columns;
    }
}
