// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.botprime.EntityBOTPrimeBody;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.entity.mob.botprime.EntityWormBaseNT;
import com.hbm.main.ResourceManager;
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

public class RenderBOTPrime<T extends EntityWormBaseNT>
        extends EntityRenderer<T, RenderBOTPrime.State> implements ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject mesh;
    private final RenderType type;

    private RenderBOTPrime(
            EntityRendererProvider.Context context, HFRWavefrontObject mesh, Identifier texture) {
        super(context);
        this.mesh = mesh;
        this.type = RenderTypes.entityCutout(texture);
        this.shadowStrength = 0F;
    }

    public static RenderBOTPrime<EntityBOTPrimeHead> head(EntityRendererProvider.Context context) {
        return new RenderBOTPrime<>(
                context, ResourceManager.bot_prime_head, ResourceManager.mark_zero_head_tex);
    }

    public static RenderBOTPrime<EntityBOTPrimeBody> body(EntityRendererProvider.Context context) {
        return new RenderBOTPrime<>(
                context, ResourceManager.bot_prime_body, ResourceManager.mark_zero_body_tex);
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
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
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch - 90F));
        collector.submitCustomGeometry(
                poseStack, this.type, (pose, buffer) -> this.mesh.render(pose, buffer, light, -1));
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
    }
}
