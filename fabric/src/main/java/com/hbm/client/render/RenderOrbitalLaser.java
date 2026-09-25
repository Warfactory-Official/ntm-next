// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.logic.EntityOrbitalLaser;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public final class RenderOrbitalLaser extends EntityRenderer<EntityOrbitalLaser, EntityRenderState>
        implements ConcurrentRenderStateExtraction {

    public RenderOrbitalLaser(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityOrbitalLaser entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntityOrbitalLaser entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(
            EntityRenderState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        collector.submitCustomGeometry(
                pose, BeamRenderTypes.ADDITIVE_CULL, RenderOrbitalLaser::beam);
        super.submit(state, pose, collector, camera);
    }

    private static void beam(PoseStack.Pose pose, VertexConsumer buffer) {
        for (int i = 0; i < OrbitalLaserGeometry.VERTEX_COUNT; i++) {
            int xyz = i * 3;
            Vertices.emit(
                    buffer,
                    pose,
                    OrbitalLaserGeometry.XYZ[xyz],
                    OrbitalLaserGeometry.XYZ[xyz + 1],
                    OrbitalLaserGeometry.XYZ[xyz + 2],
                    OrbitalLaserGeometry.ARGB[i]);
        }
    }
}
