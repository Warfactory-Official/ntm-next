// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.missile.EntitySoyuz;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class RenderSoyuz extends EntityRenderer<EntitySoyuz, RenderSoyuz.State>
        implements ConcurrentRenderStateExtraction {

    public RenderSoyuz(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntitySoyuz entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntitySoyuz entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntitySoyuz entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = SoyuzMesh.wrapSkin(entity.getSkin());
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        SoyuzMesh.submit(poseStack, collector, state.lightCoords, state.skin);
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        int skin;
    }
}
