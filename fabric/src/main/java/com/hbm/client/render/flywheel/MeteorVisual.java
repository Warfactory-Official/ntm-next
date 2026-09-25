// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MeteorVisual extends HbmDynamicEntityVisual<EntityMeteor> {
    private static final int WHITE = ARGB.color(255, 255, 255, 255);
    private static final float[][] CUBE = {
        {
            -0.5F, -0.5F, -0.5F, 1F, 0F, +0.5F, -0.5F, -0.5F, 0F, 0F, +0.5F, +0.5F, -0.5F, 0F, 1F,
            -0.5F, +0.5F, -0.5F, 1F, 1F
        },
        {
            -0.5F, -0.5F, +0.5F, 1F, 0F, -0.5F, -0.5F, -0.5F, 0F, 0F, -0.5F, +0.5F, -0.5F, 0F, 1F,
            -0.5F, +0.5F, +0.5F, 1F, 1F
        },
        {
            +0.5F, -0.5F, +0.5F, 1F, 0F, -0.5F, -0.5F, +0.5F, 0F, 0F, -0.5F, +0.5F, +0.5F, 0F, 1F,
            +0.5F, +0.5F, +0.5F, 1F, 1F
        },
        {
            +0.5F, -0.5F, -0.5F, 1F, 0F, +0.5F, -0.5F, +0.5F, 0F, 0F, +0.5F, +0.5F, +0.5F, 0F, 1F,
            +0.5F, +0.5F, -0.5F, 1F, 1F
        },
        {
            -0.5F, -0.5F, +0.5F, 1F, 0F, +0.5F, -0.5F, +0.5F, 0F, 0F, +0.5F, -0.5F, -0.5F, 0F, 1F,
            -0.5F, -0.5F, -0.5F, 1F, 1F
        },
        {
            +0.5F, +0.5F, +0.5F, 1F, 0F, -0.5F, +0.5F, +0.5F, 0F, 0F, -0.5F, +0.5F, -0.5F, 0F, 1F,
            +0.5F, +0.5F, -0.5F, 1F, 1F
        }
    };

    private static final float AXIS = (float) (1D / Math.sqrt(3D));
    private static final float REACH = (float) (2.5D * Math.sqrt(3D));
    private static final Model MODEL =
            new SingleMeshModel(
                    cube(),
                    SimpleMaterial.builder()
                            .texture(Library.id("textures/entity/meteor.png"))
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .backfaceCulling(false)
                            .useOverlay(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance cube;
    private final Matrix4f pose = new Matrix4f();

    public MeteorVisual(VisualizationContext ctx, EntityMeteor entity, float partialTick) {
        super(ctx, entity, partialTick);
        cube = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        cube.overlay(OverlayTexture.NO_OVERLAY);
        cube.light(LightCoordsUtil.FULL_BRIGHT);
        cube.colorArgb(WHITE);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Mesh cube() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(CUBE.length);
        for (float[] face : CUBE) {
            for (int v = 0; v < 4; v++) {
                int i = v * 5;
                mesh.vertex(face[i], face[i + 1], face[i + 2], face[i + 3], face[i + 4], WHITE);
            }
        }
        return mesh.build();
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return sphereVisible(frustum, 0F, REACH);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float spin = (entity.tickCount % 360 + partialTick) * 10F;
        cube.setTransform(
                pose.translation(visualPos.x, visualPos.y, visualPos.z)
                        .rotateX((float) Math.PI)
                        .rotate((float) Math.toRadians(spin), AXIS, AXIS, AXIS)
                        .scale(5F)
                        .rotateZ((float) Math.PI));
        cube.setChanged();
    }

    @Override
    protected void _delete() {
        cube.delete();
    }
}
