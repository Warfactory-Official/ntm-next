// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CloudTomVisual extends HbmDynamicEntityVisual<EntityCloudTom> {
    private static final int SEGMENTS = 16;
    private static final int CURTAINS = 5;
    private static final int HEIGHT = 20;
    private static final int DEPTH = 20;
    private static final float TOP = HEIGHT + (CURTAINS - 1) * 10F;
    private static final int SOLID = ARGB.color(255, 255, 255, 255);
    private static final int CLEAR = ARGB.color(0, 255, 255, 255);
    private static final Model MODEL =
            new SingleMeshModel(
                    curtains(),
                    SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL)
                            .texture(ResourceManager.tomblast_tex)
                            .mipmap(false)
                            .useOverlay(false)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final UvTransformedInstance wall;
    private final Matrix4f pose = new Matrix4f();

    public CloudTomVisual(VisualizationContext ctx, EntityCloudTom entity, float partialTick) {
        super(ctx, entity, partialTick);
        wall = instancerProvider().instancer(InstanceTypes.UV_TRANSFORMED, MODEL).createInstance();
        wall.overlay(OverlayTexture.NO_OVERLAY);
        wall.light(LightCoordsUtil.FULL_BRIGHT);
        wall.colorArgb(SOLID);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static Mesh curtains() {
        PackedQuadMesh.Builder mesh = PackedQuadMesh.builder(SEGMENTS * CURTAINS);
        float angle = (float) Math.toRadians(360D / SEGMENTS);

        for (int i = 0; i < SEGMENTS; i++) {
            for (int j = 0; j < CURTAINS; j++) {
                double mod = 1D - j * 0.025D;
                double h = HEIGHT + j * 10D;

                double off = 1D / j;

                Vec3 vector = new Vec3(1D, 0D, 0D).yRot(angle * i);
                mesh.vertex(vector.x * mod, h, vector.z * mod, 0D, 1D + off, CLEAR);
                mesh.vertex(vector.x * mod, -DEPTH, vector.z * mod, 0D, off, SOLID);

                vector = vector.yRot(angle);
                mesh.vertex(vector.x * mod, -DEPTH, vector.z * mod, 1D, off, SOLID);
                mesh.vertex(vector.x * mod, h, vector.z * mod, 1D, 1D + off, CLEAR);
            }
        }
        return mesh.build();
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        float scale = entity.age + 1F;
        float half = (TOP + DEPTH) * .5F;
        return sphereVisible(frustum, (TOP - DEPTH) * .5F, Mth.sqrt(scale * scale + half * half));
    }

    @Override
    protected void frame(Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float scale = entity.age + partialTick;
        float scroll = -(entity.tickCount + partialTick) * 0.05F;
        wall.setTransform(
                pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(scale, 1F, scale));
        wall.uvRegion(0F, scroll, 1F, 1F);
        wall.setChanged();
    }

    @Override
    protected void _delete() {
        wall.delete();
    }
}
