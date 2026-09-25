// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;

public class RenderEMPBlast extends EntityRenderer<EntityEMPBlast, RenderEMPBlast.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE = VortexRenderTypes.cutout(ResourceManager.emp_ring_tex);

    public RenderEMPBlast(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityEMPBlast entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityEMPBlast entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.scale;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.scale, 1F, state.scale);
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) ->
                        ResourceManager.emp_ring.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float scale;
    }
}
