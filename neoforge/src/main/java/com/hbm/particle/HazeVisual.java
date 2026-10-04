// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.particle.FogQuadMesh;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class HazeVisual implements EffectVisual<ParticleHaze>, SimpleTickableVisual {
    private static final Model MODEL =
            new SingleMeshModel(
                    FogQuadMesh.INSTANCE,
                    SimpleMaterial.builder()
                            .texture(ParticleHaze.TEXTURE)
                            .mipmap(false)
                            .useOverlay(false)
                            .transparency(Transparency.ORDER_INDEPENDENT)
                            .writeMask(WriteMask.COLOR)
                            .cutout(new SimpleCutoutShader(Library.id("cutout/zero.glsl")))
                            .light(LightShaders.FLAT)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .backfaceCulling(false)
                            .build());

    private final ParticleHaze particle;
    private final BillboardInstance[] instances = new BillboardInstance[ParticleHaze.QUADS.length];
    private final Vec3i origin;

    public HazeVisual(VisualizationContext context, ParticleHaze particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        var instancer = context.instancerProvider().instancer(InstanceTypes.BILLBOARD, MODEL);
        for (int i = 0; i < instances.length; i++) {
            ParticleHaze.Quad quad = ParticleHaze.QUADS[i];
            instances[i] = instancer.createInstance();
            instances[i]
                    .size(2F * quad.sizeFactor() * particle.scale())
                    .uvRegion(0F, 0F, 1F, 1F)
                    .light(LightCoordsUtil.pack(15, 0));
        }
        writeTick();
    }

    @Override
    public void tick(Context context) {
        writeTick();
    }

    private void writeTick() {
        boolean visible = particle.isAlive();
        float x = (float) (particle.x - origin.getX());
        float y = (float) (particle.y - origin.getY());
        float z = (float) (particle.z - origin.getZ());
        float alpha = particle.alpha();
        for (int i = 0; i < instances.length; i++) {
            BillboardInstance instance = instances[i];
            instance.setVisible(visible);
            if (!visible) continue;
            ParticleHaze.Quad quad = ParticleHaze.QUADS[i];
            instance.position(x + quad.offsetX(), y + quad.offsetY(), z + quad.offsetZ());
            instance.color(1F, 1F, 1F, alpha).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance instance : instances) instance.delete();
    }
}
