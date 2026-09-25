// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class RenderFBIDrone extends EntityRenderer<EntityFBIDrone, RenderFBIDrone.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.quadcopter_tex);

    public RenderFBIDrone(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityFBIDrone entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = (float) (new Random(entity.getId()).nextDouble() * 360D);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0D, 0.25D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) -> ResourceManager.quadcopter.render(pose, buffer, light, -1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
    }
}
