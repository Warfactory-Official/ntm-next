// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderMassStorage
        implements BlockEntityRenderer<BlockEntityMassStorage, RenderMassStorage.State>,
                ConcurrentRenderStateExtraction {
    public static final int TEXT_COLOR = 0xFF00FF00;

    public static final int VIEW_DISTANCE = 32;

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public RenderMassStorage(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.font = context.font();
    }

    public static String countText(int stockpile, boolean unicode) {
        if (stockpile >= 100_000_000 || (stockpile >= 1_000_000 && unicode)) {
            return String.format("%.0fM", stockpile / 1_000_000f);
        }
        if (stockpile >= 1_000_000) return String.format("%.1fM", stockpile / 1_000_000f);
        if (stockpile >= 100_000 || (stockpile >= 10_000 && unicode)) {
            return String.format("%.0fK", stockpile / 1000f);
        }
        if (stockpile >= 10_000) return String.format("%.1fK", stockpile / 1000f);
        return String.valueOf(stockpile);
    }

    private static void barVertex(
            VertexConsumer buf, PoseStack.Pose pose, float x, float y, int color) {
        Vertices.emit(buf, pose, x, y, 0F, color, 0F, 0F, LightCoordsUtil.FULL_BRIGHT, 0F, 0F, 1F);
    }

    public static float panelAngle(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180F;
            case WEST -> -90F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    public static void panelPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.mulPose(Axis.YP.rotationDegrees(panelAngle(facing)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        poseStack.translate(0F, 0F, -0.005F);
        poseStack.scale(1F / 16F, 1F / 16F, -0.0001F);
    }

    public static void iconPose(PoseStack poseStack) {
        poseStack.translate(4F, 2.5F, 0F);
        poseStack.scale(8F / 16F, 8F / 16F, 1F);

        poseStack.translate(8F, 8F, 0F);
        poseStack.scale(16F, -16F, 16F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public void extractRenderState(
            BlockEntityMassStorage storage,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                storage, state, partialTicks, cameraPosition, breakProgress);

        state.iconOnly = HbmBlockEntityVisual.hasVisual(storage);
        var type = storage.getItem(BlockEntityMassStorage.SLOT_TYPE);
        if (type.isEmpty()) {
            state.type = null;
            return;
        }
        if (state.type == null) state.type = new ItemStackRenderState();
        itemModelResolver.updateForTopItem(
                state.type, type, ItemDisplayContext.GUI, storage.getLevel(), null, 0);
        state.facing = storage.getBlockState().getValue(BlockMassStorage.FACING);
        state.fraction = (float) storage.getStockpile() / storage.getCapacity();
        state.count =
                countText(
                        storage.getStockpile(),
                        Minecraft.getInstance().options.forceUnicodeFont().get());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.type == null) return;

        poseStack.pushPose();
        panelPose(poseStack, state.facing);

        poseStack.pushPose();
        iconPose(poseStack);
        state.type.submit(
                poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (state.iconOnly) {
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        poseStack.scale(4F / 16F, 4F / 16F, 4F / 16F);
        collector.submitText(
                poseStack,
                32F - font.width(state.count) / 2F,
                44F,
                FormattedCharSequence.forward(state.count, Style.EMPTY),
                true,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                TEXT_COLOR,
                0,
                0);
        poseStack.popPose();

        int barColor = ARGB.colorFromFloat(1F, 1F - state.fraction, state.fraction, 0F);
        collector.submitCustomGeometry(
                poseStack,
                WorldRenderPipeline.UNTEXTURED_CULL,
                (pose, buf) -> {
                    float maxX = 2F + state.fraction * 12F;
                    barVertex(buf, pose, 2F, 14F, barColor);
                    barVertex(buf, pose, maxX, 14F, barColor);
                    barVertex(buf, pose, maxX, 13.5F, barColor);
                    barVertex(buf, pose, 2F, 13.5F, barColor);
                });

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public @Nullable ItemStackRenderState type;
        public Direction facing = Direction.NORTH;
        public float fraction;
        public String count = "0";
        public boolean iconOnly;
    }
}
