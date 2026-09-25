// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityBullet;
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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

public class RenderBullet extends EntityRenderer<EntityBullet, RenderBullet.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType PLAIN =
            RenderTypes.entityCutoutCull(ResourceManager.bullet_tex);
    private static final RenderType CHOPPER =
            RenderTypes.entityCutoutCull(ResourceManager.bullet_chopper_tex);
    private static final RenderType CRITICAL =
            RenderTypes.entityCutoutCull(ResourceManager.bullet_critical_tex);

    public RenderBullet(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityBullet entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.roll = new Random(entity.getId()).nextInt(360);
        state.type = entity.getIsChopper() ? CHOPPER : entity.getIsCritical() ? CRITICAL : PLAIN;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180F));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.roll));
        RenderBoxModel.submit(
                poseStack, collector, state.type, RenderBoxModel.BULLET, state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        float roll;
        RenderType type = PLAIN;
    }
}
