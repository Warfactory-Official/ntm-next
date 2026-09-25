// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class RenderSoyuzCapsule extends EntityRenderer<EntitySoyuzCapsule, RenderSoyuzCapsule.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType LANDER =
            RenderTypes.entityCutoutCull(ResourceManager.soyuz_lander_tex);
    private static final RenderType CHUTE =
            RenderTypes.entityCutoutCull(ResourceManager.soyuz_chute_tex);
    private static final int CAPSULE_PART = ResourceManager.soyuz_lander.partId("Capsule");
    private static final int CHUTE_PART = ResourceManager.soyuz_lander.partId("Chute");
    private static final int PIVOT = 7;

    public RenderSoyuzCapsule(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntitySoyuzCapsule entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntitySoyuzCapsule entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntitySoyuzCapsule entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.time = (entity.level().getGameTime() % (Math.PI * 20D)) + partialTicks;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0D, PIVOT, 0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(state.time * 0.05D) * 5D)));
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        (float) (Math.sin(state.time * 0.05D + Math.PI * 0.5D) * 5D)));
        poseStack.translate(0D, -PIVOT, 0D);
        collector.submitCustomGeometry(
                poseStack,
                LANDER,
                (pose, buffer) ->
                        ResourceManager.soyuz_lander.renderPart(
                                pose, buffer, light, -1, CAPSULE_PART));
        collector.submitCustomGeometry(
                poseStack,
                CHUTE,
                (pose, buffer) ->
                        ResourceManager.soyuz_lander.renderPart(
                                pose, buffer, light, -1, CHUTE_PART));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        double time;
    }
}
