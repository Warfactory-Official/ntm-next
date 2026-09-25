// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

public class RenderMinerRocket<T extends Entity> extends EntityRenderer<T, EntityRenderState>
        implements ConcurrentRenderStateExtraction {

    private final RenderType type;

    private RenderMinerRocket(EntityRendererProvider.Context context, RenderType type) {
        super(context);
        this.type = type;
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    public static <T extends Entity> RenderMinerRocket<T> bobmazon(
            EntityRendererProvider.Context context) {
        return new RenderMinerRocket<>(
                context, WorldRenderPipeline.oneSidedCutout(ResourceManager.bobmazon_tex));
    }

    @Override
    public boolean shouldRender(T entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(
            EntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) -> ResourceManager.miner_rocket.render(pose, buffer, light, -1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
