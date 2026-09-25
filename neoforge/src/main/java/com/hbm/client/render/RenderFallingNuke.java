// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

public class RenderFallingNuke extends EntityRenderer<EntityFallingNuke, RenderFallingNuke.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.custom_nuke_tex);

    public RenderFallingNuke(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityFallingNuke entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityFallingNuke entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        state.pitch = pitch < -80F ? 0F : pitch;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) -> ResourceManager.lil_boy.render(pose, buffer, light, -1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float pitch;
    }
}
