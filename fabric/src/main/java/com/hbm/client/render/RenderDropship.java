// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.missile.EntitySatellitePod;
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

public final class RenderDropship extends EntityRenderer<EntitySatellitePod, RenderDropship.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.dropship_tex);
    private static final int POD = ResourceManager.dropship.partId("Pod");
    private static final int LEG = ResourceManager.dropship.partId("Leg");

    public RenderDropship(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntitySatellitePod entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntitySatellitePod entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntitySatellitePod entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.legs = entity.prevLegs + (entity.legs - entity.prevLegs) * partialTicks;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();
        collector.submitCustomGeometry(
                pose,
                TYPE,
                (p, buffer) -> ResourceManager.dropship.renderPart(p, buffer, light, -1, POD));
        for (int i = 0; i < 4; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(45F + 90F * i));
            pose.translate(0.5D, 1.75D, 0D);
            pose.mulPose(Axis.ZP.rotationDegrees(150F * (1F - state.legs)));
            pose.translate(-0.5D, -1.75D, 0D);
            collector.submitCustomGeometry(
                    pose,
                    TYPE,
                    (p, buffer) -> ResourceManager.dropship.renderPart(p, buffer, light, -1, LEG));
            pose.popPose();
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float legs;
    }
}
