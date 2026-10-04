// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.RenderTextures;
import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.particle.ParticleAmatFlash;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;

public final class AmatFlashVisual implements EffectVisual<ParticleAmatFlash>, SimpleDynamicVisual {
    private static final Model MODEL =
            new SingleMeshModel(
                    mesh(), AdditiveParticleModels.material(RenderTextures.WHITE, true, true));

    private final ParticleAmatFlash particle;
    private final TransformedInstance instance;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public AmatFlashVisual(
            VisualizationContext context, ParticleAmatFlash particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MODEL)
                        .createInstance();
        writeFrame(partialTick);
    }

    private static Mesh mesh() {
        PackedQuadMesh.Builder mesh =
                PackedQuadMesh.builder(ParticleAmatFlash.SPIKES * 2).normal(0F, 0F, 1F);
        float[] rim = ParticleAmatFlash.RIM;
        for (int i = 0; i < ParticleAmatFlash.SPIKES; i++) {
            int b = i * 9;
            mesh.vertex(0D, 0D, 0D, 0xFFFFFFFF);
            mesh.vertex(rim[b], rim[b + 1], rim[b + 2], 0x00FFFFFF);
            mesh.vertex(rim[b + 3], rim[b + 4], rim[b + 5], 0x00FFFFFF);
            mesh.vertex(rim[b + 6], rim[b + 7], rim[b + 8], 0x00FFFFFF);

            mesh.vertex(0D, 0D, 0D, 0xFFFFFFFF);
            mesh.vertex(rim[b + 6], rim[b + 7], rim[b + 8], 0x00FFFFFF);
            mesh.vertex(rim[b], rim[b + 1], rim[b + 2], 0x00FFFFFF);
            mesh.vertex(rim[b], rim[b + 1], rim[b + 2], 0x00FFFFFF);
        }
        return mesh.build();
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        instance.setTransform(pose.translation(x, y, z).scale(particle.extent(partialTick)));
        instance.colorArgb(ARGB.colorFromFloat(particle.coreAlpha(partialTick), 1F, 1F, 1F));
        instance.light(particle.light(partialTick)).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
