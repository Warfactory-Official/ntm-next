// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

public class RenderDeathBlast extends RenderLaserColumn<EntityDeathBlast> {

    public RenderDeathBlast(EntityRendererProvider.Context context) {
        super(context, 0x00FF00, 0xFF00FF);
    }

    @Override
    public void extractRenderState(EntityDeathBlast entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = (entity.tickCount + partialTicks) / EntityDeathBlast.maxAge;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        float alpha = state.age;
        float scale = Math.max(10F - 10F * state.age, 0F);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        submitSphere(poseStack, collector, ARGB.colorFromFloat(alpha, 0.05F, 1F, 0.05F), false);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        for (int i = 0; i < 8; i++) {
            submitSphere(
                    poseStack, collector, ARGB.colorFromFloat(alpha * 0.125F, 0F, 1F, 0F), true);
            poseStack.scale(1.05F, 1.05F, 1.05F);
        }
        poseStack.popPose();
    }

    private static void submitSphere(
            PoseStack poseStack, SubmitNodeCollector collector, int color, boolean additive) {
        collector.submitCustomGeometry(
                poseStack,
                additive ? CloudRenderTypes.ADDITIVE : CloudRenderTypes.TRANSLUCENT,
                (pose, buffer) ->
                        ResourceManager.black_hole.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, color));
    }
}
