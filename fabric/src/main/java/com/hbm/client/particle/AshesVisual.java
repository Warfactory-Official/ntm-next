// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.particle.ParticleAshes;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import org.joml.Matrix4f;

public final class AshesVisual implements EffectVisual<ParticleAshes>, SimpleDynamicVisual {
    private static final int WHITE = 0xFFFFFFFF;
    private static final Model FLAT =
            new SingleMeshModel(
                    PackedQuadMesh.builder(1)
                            .normal(0F, 1F, 0F)
                            .vertex(1D, 0D, 1D, 1D, 1D, WHITE)
                            .vertex(1D, 0D, -1D, 1D, 0D, WHITE)
                            .vertex(-1D, 0D, -1D, 0D, 0D, WHITE)
                            .vertex(-1D, 0D, 1D, 0D, 1D, WHITE)
                            .build(),
                    ParticleModels.LIT_PARTICLE_BASE.meshes().getFirst().material());

    private final ParticleAshes particle;
    private final ParticleInstance falling;
    private final TransformedInstance settled;
    private final Vec3i origin;
    private final Matrix4f pose = new Matrix4f();

    public AshesVisual(VisualizationContext context, ParticleAshes particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.falling =
                context.instancerProvider()
                        .instancer(ParticleInstance.TYPE, ParticleModels.LIT_PARTICLE_BASE)
                        .createInstance();
        this.settled =
                context.instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FLAT)
                        .createInstance();
        falling.uvRegion(0F, 0F, 1F, 1F);
        writeFrame(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean flat = particle.isAlive() && particle.settled();
        boolean fallingVisible = particle.isAlive() && !flat;
        falling.setVisible(fallingVisible);
        settled.setVisible(flat);
        if (!particle.isAlive()) return;

        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        float scale = particle.size();
        float spin = (float) Math.toRadians(particle.spin(partialTick));
        int shade = (int) (particle.shade() * 255F);
        int alpha = (int) (particle.alpha(partialTick) * 255F);
        int color = alpha << 24 | shade << 16 | shade << 8 | shade;
        int light = particle.light(partialTick);

        if (flat) {
            settled.setTransform(pose.translation(x, y - 0.09F, z).rotateY(spin).scale(scale));
            settled.colorArgb(color).light(light).setChanged();
        } else {
            falling.position(x, y, z);
            falling.previousPosition(x, y, z);
            falling.size(2F * scale);
            falling.animation(spin, spin, 0F, 1F);
            falling.colorArgb(color).light(light).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        falling.delete();
        settled.delete();
    }
}
