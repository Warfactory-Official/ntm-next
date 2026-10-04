// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FalloutRainColumns.Frame;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Arrays;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FalloutRainVisual extends HbmDynamicEntityVisual<EntityFalloutRain> {
    private static final Model MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.builder(1)
                            .normal(0F, 1F, 0F)
                            .vertex(0D, 0D, 0D, 0D, 0D, 0xFFFFFFFF)
                            .vertex(1D, 0D, 0D, 1D, 0D, 0xFFFFFFFF)
                            .vertex(1D, 1D, 0D, 1D, 1D, 0xFFFFFFFF)
                            .vertex(0D, 1D, 0D, 0D, 1D, 0xFFFFFFFF)
                            .build(),
                    SimpleMaterial.builder()
                            .texture(Library.id("textures/entity/fallout.png"))
                            .mipmap(false)
                            .useOverlay(false)
                            .transparency(Transparency.ORDER_INDEPENDENT)
                            .writeMask(WriteMask.COLOR)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.FLAT)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .backfaceCulling(false)
                            .build());

    private volatile @Nullable Frame rainFrame;
    private UvTransformedInstance[] instances = new UvTransformedInstance[0];
    private final Matrix4f pose = new Matrix4f();
    private int active;

    public FalloutRainVisual(
            VisualizationContext context, EntityFalloutRain entity, float partialTick) {
        super(context, entity, partialTick);
        FalloutRainVisuals.add(this);
    }

    EntityFalloutRain entity() {
        return entity;
    }

    boolean hasFrame() {
        return rainFrame != null;
    }

    void publishFrame(@Nullable Frame frame) {
        rainFrame = frame;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        return true;
    }

    @Override
    protected void frame(Context context) {
        Frame frame = rainFrame;
        int count = frame == null ? 0 : frame.count();
        if (count > instances.length) {
            int old = instances.length;
            instances = Arrays.copyOf(instances, Math.max(count, old * 2 + 1));
            var instancer = instancerProvider().instancer(InstanceTypes.UV_TRANSFORMED, MODEL);
            for (int i = old; i < instances.length; i++) {
                instances[i] = instancer.createInstance();
                instances[i].setVisible(false);
            }
        }
        if (frame != null) {
            float baseX = (float) (frame.entityX() - renderOrigin().getX());
            float baseY = (float) (frame.entityY() - renderOrigin().getY());
            float baseZ = (float) (frame.entityZ() - renderOrigin().getZ());
            float[] vertices = frame.vertices();
            int[] colors = frame.colors();
            int[] lights = frame.lights();
            for (int i = 0; i < count; i++) {
                int b = i * 10;
                float x0 = vertices[b], y0 = vertices[b + 1], z0 = vertices[b + 2];
                float x1 = vertices[b + 3], y1 = vertices[b + 4], z1 = vertices[b + 5];
                float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
                UvTransformedInstance instance = instances[i];
                instance.setVisible(true);
                instance.setTransform(
                        pose.identity()
                                .m00(dx)
                                .m02(dz)
                                .m11(dy)
                                .m30(baseX + x0)
                                .m31(baseY + y0)
                                .m32(baseZ + z0));
                instance.uvRegion(
                        vertices[b + 6],
                        vertices[b + 7],
                        vertices[b + 8] - vertices[b + 6],
                        vertices[b + 9] - vertices[b + 7]);
                instance.colorArgb(colors[i]).light(lights[i]).setChanged();
            }
        }
        for (int i = count; i < active; i++) instances[i].setVisible(false);
        active = count;
    }

    @Override
    protected void _delete() {
        FalloutRainVisuals.remove(this);
        for (UvTransformedInstance instance : instances) instance.delete();
    }
}
