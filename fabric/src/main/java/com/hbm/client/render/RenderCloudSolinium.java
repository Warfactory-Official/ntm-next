// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityCloudSolinium;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;

public class RenderCloudSolinium
        extends EntityRenderer<EntityCloudSolinium, RenderCloudSolinium.State>
        implements ConcurrentRenderStateExtraction {

    private static final int COLOR = 0x27FFDA;
    private static final int CORE = ARGB.colorFromFloat(1F, fr(COLOR), fg(COLOR), fb(COLOR));
    private static final int SHELL = ARGB.colorFromFloat(0.125F, fr(COLOR), fg(COLOR), fb(COLOR));

    public RenderCloudSolinium(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static float fr(int color) {
        return (color >> 16 & 0xFF) / 255F;
    }

    private static float fg(int color) {
        return (color >> 8 & 0xFF) / 255F;
    }

    private static float fb(int color) {
        return (color & 0xFF) / 255F;
    }

    @Override
    protected boolean affectedByCulling(EntityCloudSolinium entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityCloudSolinium entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.age + partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.age, state.age, state.age);

        RenderCloudFleija.submitSphere(
                poseStack, collector, CloudRenderTypes.OPAQUE_UNCULLED, CORE);

        double outerScale = 1.025;
        for (int i = 0; i < 3; i++) {
            poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
            RenderCloudFleija.submitSphere(poseStack, collector, CloudRenderTypes.ADDITIVE, SHELL);
        }
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float age;
    }
}
