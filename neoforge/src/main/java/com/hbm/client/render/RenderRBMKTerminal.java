// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.gui.ScreenRBMKTerminal;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import com.hbm.util.BobMathUtil;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKTerminal
        implements BlockEntityRenderer<BlockEntityRBMKTerminal, RenderRBMKTerminal.State>,
                ConcurrentRenderStateExtraction {
    public static final int LINES = 18;

    private static final float SCALE = 1F / 250F;
    private static final String PREFIX = "> ";
    private static final int MAX_WIDTH = 172;
    private static final int GREEN = ARGB.opaque(0x00ff00);
    private static final int AMBER = ARGB.opaque(0xff8000);

    private final Font font;

    public RenderRBMKTerminal(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKTerminal be) {
        return new AABB(be.getBlockPos()).inflate(1.0D);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private String clip(String label, boolean first, boolean caret) {
        StringBuilder builder = new StringBuilder(40);
        if (first || !label.isEmpty()) builder.append(PREFIX);

        int width = font.width(PREFIX);
        for (int j = 0; j < label.length(); j++) {
            char c = label.charAt(j);
            width += font.width(String.valueOf(c));
            if (width <= MAX_WIDTH) {
                builder.append(c);
            } else {
                break;
            }
        }

        if (first && caret && font.width(builder.toString()) + font.width("_") <= MAX_WIDTH) {
            builder.append("_");
        }
        return builder.toString();
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKTerminal be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);
        state.color = be.doesRepeat ? AMBER : GREEN;

        state.caret = ScreenRBMKTerminal.isEditing(be) && BobMathUtil.getBlink();
        for (int i = 0; i < LINES; i++) {
            String label = i == 0 ? ScreenRBMKTerminal.getWorkingLine(be) : be.history[i - 1];
            state.labels[i] = label == null ? "" : label;
        }
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

        int light = state.lightCoords;

        poseStack.translate(0.25, 0.0, 0.0);

        poseStack.translate(0.0635, 0.125, 0.0625 * 5.5);

        for (int i = 0; i < LINES; i++) {
            poseStack.translate(0.0, 10 * SCALE, 0.0);

            poseStack.pushPose();
            poseStack.scale(SCALE, -SCALE, SCALE);
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));

            FormattedCharSequence line =
                    Component.literal(clip(state.labels[i], i == 0, state.caret))
                            .getVisualOrderText();
            collector.submitText(
                    poseStack,
                    0,
                    -font.lineHeight / 2,
                    line,
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    state.color,
                    0,
                    0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final String[] labels = new String[LINES];
        public boolean caret;
        public float yaw;
        public int color = GREEN;
    }
}
