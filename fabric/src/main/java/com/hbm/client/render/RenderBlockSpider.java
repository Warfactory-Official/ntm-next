// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityBlockSpider;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class RenderBlockSpider extends EntityRenderer<EntityBlockSpider, RenderBlockSpider.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.blockspider_tex);
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    private static final int[] ODD =
            ResourceManager.blockspider.partIds("Leg1", "Leg3", "Leg5", "Leg7");
    private static final int[] EVEN =
            ResourceManager.blockspider.partIds("Leg2", "Leg4", "Leg6", "Leg8");

    private final BlockModelResolver blockModelResolver;

    public RenderBlockSpider(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 1.0F;
    }

    private static void legPart(
            PoseStack poseStack, SubmitNodeCollector collector, int light, int part) {
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) ->
                        ResourceManager.blockspider.renderPart(pose, buffer, light, -1, part));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityBlockSpider entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.swing =
                -(Mth.cos(entity.walkAnimation.position(partialTicks) * 0.6662F * 2F) * 0.4F)
                        * entity.walkAnimation.speed(partialTicks)
                        * 57.3F;
        blockModelResolver.update(state.disguise, entity.getDisguise(), DISPLAY);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {

        if (state.disguise.isEmpty()) {
            super.submit(state, poseStack, collector, camera);
            return;
        }

        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyYaw));

        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.pushPose();
        poseStack.translate(0D, state.swing * 0.005D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.swing));
        for (int leg : ODD) legPart(poseStack, collector, light, leg);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0D, state.swing * -0.005D, 0D);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.swing));
        for (int leg : EVEN) legPart(poseStack, collector, light, leg);
        poseStack.popPose();

        poseStack.pushPose();

        poseStack.translate(-0.5D, 0.25D, -0.5D);
        state.disguise.submit(
                poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public final BlockModelRenderState disguise = new BlockModelRenderState();
        float bodyYaw;
        float swing;
    }
}
