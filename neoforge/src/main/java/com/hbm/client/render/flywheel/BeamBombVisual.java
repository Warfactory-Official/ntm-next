// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.projectile.EntityB92Beam;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BeamBombVisual extends HbmDynamicEntityVisual<EntityB92Beam> {
    private static final float RADIUS = 0.175F;
    private static final int LENGTH = 2;
    private static final int LAYERS = 8;
    private static final int WHITE = ARGB.color(255, 255, 255, 255);
    private static final Model MODEL =
            new SingleMeshModel(
                    beam(),
                    SimpleMaterial.builderOf(EffectVisuals.Shared.BEAM_ADDITIVE)
                            .useLight(true)
                            .light(LightShaders.FLAT)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance bolt;
    private final Matrix4f pose = new Matrix4f();

    public BeamBombVisual(VisualizationContext ctx, EntityB92Beam entity, float partialTick) {
        super(ctx, entity, partialTick);
        bolt = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        bolt.overlay(OverlayTexture.NO_OVERLAY);
        bolt.colorArgb(WHITE);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Mesh beam() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder((LAYERS + 1) * 4).normal(0F, 0F, 1F);
        for (int layer = 0; layer <= LAYERS; layer++) {
            float o = RADIUS / LAYERS * layer;
            float shade = Math.max(1F - o * 8.333F, 0F);
            int color = ARGB.colorFromFloat(1F, shade, shade, 1F);
            wall(mesh, color, o, -o, o, o);
            wall(mesh, color, -o, -o, o, -o);
            wall(mesh, color, -o, o, -o, -o);
            wall(mesh, color, o, o, -o, o);
        }
        return mesh.build();
    }

    private static void wall(
            PackedQuadMesh.Builder mesh, int color, float x0, float y0, float x1, float y1) {
        mesh.vertex(x0, y0, 0D, color);
        mesh.vertex(x1, y1, 0D, color);
        mesh.vertex(x1, y1, LENGTH, color);
        mesh.vertex(x0, y0, LENGTH, color);
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(
                frustum, 0F, LENGTH + RADIUS + (float) entity.getDeltaMovement().length());
    }

    @Override
    protected void frame(Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        bolt.setTransform(
                pose.translation(visualPos.x, visualPos.y, visualPos.z)
                        .rotateY(yaw * Mth.DEG_TO_RAD)
                        .rotateX(-pitch * Mth.DEG_TO_RAD));
        bolt.light(computePackedLight(partialTick));
        bolt.setChanged();
    }

    @Override
    protected void _delete() {
        bolt.delete();
    }
}
