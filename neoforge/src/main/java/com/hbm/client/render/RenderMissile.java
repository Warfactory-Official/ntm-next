// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.render.loader.HFRWavefrontObject;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderMissile extends EntityRenderer<EntityMissileBaseNT, RenderMissile.State>
        implements ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject model;
    private final RenderType renderType;
    private final float scale;

    public RenderMissile(
            EntityRendererProvider.Context context,
            HFRWavefrontObject model,
            Identifier texture,
            float scale) {
        super(context);
        this.model = model;
        this.renderType = RenderTypes.entityCutoutCull(texture);
        this.scale = scale;
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityMissileBaseNT entity) {
        return false;
    }

    private static float padRoll(byte facing) {
        return switch (facing) {
            case 2 -> 90F;
            case 3 -> 270F;
            case 4 -> 180F;
            default -> 0F;
        };
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityMissileBaseNT entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.yaw = Mth.rotLerp(partialTicks, entity.renderYawO, entity.renderYaw);
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.padRoll = padRoll(entity.getFacing());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));

        poseStack.mulPose(Axis.YN.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.padRoll));
        if (scale != 1F) poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(
                poseStack, renderType, (pose, buffer) -> model.render(pose, buffer, light, -1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float yaw;
        public float pitch;
        public float padRoll;
    }
}
