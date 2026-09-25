// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.function.Function;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPress<T extends BlockEntity>
        implements BlockEntityRenderer<T, RenderPress.State>, ConcurrentRenderStateExtraction {
    private static final float STROKE = 0.875F;

    public static final ItemPose PRESS_ITEM =
            (poseStack, yaw) -> {
                poseStack.translate(0.5, 1.0, -0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(180F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.translate(0.0, 1.0F - 0.0625F * 165 / 100, 0.0);
            };

    public static final ItemPose EPRESS_ITEM =
            (poseStack, yaw) -> {
                poseStack.translate(0.5, 1.0, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.translate(1.0, 1.0F - 0.0625F * 165 / 100, 0.0);
                poseStack.translate(-1.0, -1.15, 0.0);
            };

    private final ItemModelResolver itemModelResolver;
    private final HFRWavefrontObject head;
    private final RenderType headType;
    private final float headYaw;
    private final boolean turnsWithFacing;
    private final float headBase;
    private final float headScaleXZ;
    private final int maxProgress;
    private final PressTravel<T> press;
    private final Function<T, ItemStack> inputStack;
    private final ItemPose itemPose;

    public RenderPress(
            BlockEntityRendererProvider.Context context,
            HFRWavefrontObject head,
            Identifier headTex,
            float headYaw,
            boolean turnsWithFacing,
            float headBase,
            float headScaleXZ,
            int maxProgress,
            PressTravel<T> press,
            Function<T, ItemStack> inputStack,
            ItemPose itemPose) {
        this.itemModelResolver = context.itemModelResolver();
        this.head = head;
        this.headType = RenderTypes.entityCutoutCull(headTex);
        this.headYaw = headYaw;
        this.turnsWithFacing = turnsWithFacing;
        this.headBase = headBase;
        this.headScaleXZ = headScaleXZ;
        this.maxProgress = maxProgress;
        this.press = press;
        this.inputStack = inputStack;
        this.itemPose = itemPose;
    }

    public static float facingYaw(Direction facing) {

        return Facing.yaw(facing, 0);
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
    public AABB getRenderBoundingBox(T be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 0.0625,
                pos.getY(),
                pos.getZ() - 0.0625,
                pos.getX() + 1.0625,
                pos.getY() + 3,
                pos.getZ() + 1.0625);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            T be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.visualized = HbmBlockEntityVisual.hasVisual(be);

        state.yaw =
                turnsWithFacing
                        ? headYaw + facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()))
                        : headYaw;
        if (!state.visualized) {
            double p = press.at(be, partialTicks) / maxProgress;
            state.lift = (float) (Mth.clamp(1D - p, 0D, 1D) * STROKE);
        }

        ItemStack stack = inputStack.apply(be);
        ItemStack single = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        state.arm =
                FramedItem.resolve(itemModelResolver, state.item, single, be.getLevel(), null, 0);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;

        if (!state.visualized) {
            poseStack.pushPose();
            poseStack.translate(0.5, headBase + state.lift, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            if (headScaleXZ != 1.0F) poseStack.scale(headScaleXZ, 1.0F, headScaleXZ);
            collector.submitCustomGeometry(
                    poseStack, headType, (pose, buffer) -> head.render(pose, buffer, light, -1));
            poseStack.popPose();
        }

        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            itemPose.apply(poseStack, state.yaw);
            FramedItem.decoArm(poseStack, state.arm);
            state.item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @FunctionalInterface
    public interface ItemPose {
        void apply(PoseStack poseStack, float yaw);
    }

    @FunctionalInterface
    public interface PressTravel<T> {
        double at(T be, float partialTicks);
    }

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean visualized;
        public float yaw;
        public float lift;
        public FramedItem.Arm arm = FramedItem.Arm.SPRITE;
    }
}
