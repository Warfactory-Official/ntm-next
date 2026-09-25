// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

public class RenderMaskMan extends EntityRenderer<EntityMaskMan, RenderMaskMan.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType BODY =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.maskman_tex);
    private static final RenderType IOU =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.iou_tex);

    private static final int TORSO = ResourceManager.maskman.partId("Torso");
    private static final int LEG_LEFT = ResourceManager.maskman.partId("LLeg");
    private static final int LEG_RIGHT = ResourceManager.maskman.partId("RLeg");
    private static final int ARM_LEFT = ResourceManager.maskman.partId("LArm");
    private static final int ARM_RIGHT = ResourceManager.maskman.partId("RArm");
    private static final int HEAD = ResourceManager.maskman.partId("Head");
    private static final int SKULL = ResourceManager.maskman.partId("Skull");
    private static final int NOTE = ResourceManager.maskman.partId("IOU");

    public RenderMaskMan(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityMaskMan entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        float swing = entity.walkAnimation.position(partialTicks);
        float amount = entity.walkAnimation.speed(partialTicks) * 0.5F;
        state.swing = (float) Math.toDegrees(Mth.cos(swing / 2F + (float) Math.PI) * 1.4F * amount);

        state.headYaw =
                Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.getYHeadRot())
                        - Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.wounded = entity.getHealth() < entity.getMaxHealth() / 2F;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {

        int light = state.lightCoords;

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(270F - state.bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.swing * -0.1F));

        part(poseStack, collector, BODY, TORSO, light);

        limb(poseStack, collector, LEG_LEFT, light, -0.5F, 1.75F, -0.5F, state.swing);
        limb(poseStack, collector, LEG_RIGHT, light, -0.5F, 1.75F, 0.5F, -state.swing);
        limb(poseStack, collector, ARM_LEFT, light, -0.5F, 3.75F, -1.5F, state.swing * 0.25F);
        limb(poseStack, collector, ARM_RIGHT, light, -0.5F, 3.75F, 1.5F, state.swing * -0.25F);

        poseStack.pushPose();
        poseStack.translate(0.5F, 4F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.headYaw));

        if (state.wounded) {
            part(poseStack, collector, BODY, SKULL, light);
            part(poseStack, collector, IOU, NOTE, light);
        } else {
            part(poseStack, collector, BODY, HEAD, light);
        }

        poseStack.popPose();
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void limb(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int part,
            int light,
            float x,
            float y,
            float z,
            float roll) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        part(poseStack, collector, BODY, part, light);
        poseStack.popPose();
    }

    private static void part(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            RenderType type,
            int part,
            int light) {
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.maskman.renderPart(pose, buffer, light, -1, part));
    }

    public static final class State extends EntityRenderState {
        float swing;
        float headYaw;
        float bodyYaw;
        boolean wounded;
    }
}
