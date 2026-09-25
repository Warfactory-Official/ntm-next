// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityShrapnel;
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
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public class RenderShrapnel<T extends Entity> extends EntityRenderer<T, RenderShrapnel.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.shrapnel_tex);
    private static final Vector3f TUMBLE = new Vector3f(1F, 1F, 1F).normalize();

    public RenderShrapnel(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    public static float scale(Entity entity) {
        return entity instanceof EntityShrapnel shrapnel
                        && shrapnel.getTrail() >= EntityShrapnel.TRAIL_VOLCANO
                ? 3F
                : 1F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.spin = (entity.tickCount % 360) * 10 + partialTicks;
        state.scale = scale(entity);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        poseStack.mulPose(Axis.of(TUMBLE).rotationDegrees(state.spin));
        poseStack.scale(state.scale, state.scale, state.scale);
        RenderBoxModel.submit(
                poseStack, collector, TYPE, RenderBoxModel.SHRAPNEL, state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float spin;
        float scale = 1F;
    }
}
