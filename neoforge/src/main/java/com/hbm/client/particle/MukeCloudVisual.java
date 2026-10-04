// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleMukeCloud;
import com.hbm.particle.ParticleMukeCloudBF;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;

public final class MukeCloudVisual implements EffectVisual<ParticleMukeCloud>, SimpleDynamicVisual {
    private static final float SIZE = 6F;

    private final ParticleMukeCloud particle;
    private final ParticleInstance instance;
    private final Vec3i origin;

    public MukeCloudVisual(
            VisualizationContext context, ParticleMukeCloud particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(
                                ParticleInstance.UPRIGHT,
                                particle instanceof ParticleMukeCloudBF
                                        ? ParticleModels.MUKE_CLOUD_BALEFIRE
                                        : ParticleModels.MUKE_CLOUD)
                        .createInstance();
        instance.size(SIZE).light(LightCoordsUtil.pack(15, 0));
        writeFrame(partialTick);
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
        int frame = particle.texIndex(partialTick);
        float uv = 1F / 5F;
        instance.previousPosition(x, y, z);
        instance.position(x, y, z);
        instance.animation(0F, 0F, 0F, 1F);
        instance.uvRegion(frame % 5 * uv, frame / 5 * uv, uv, uv);
        instance.color(1F, 1F, 1F, 1F).setChanged();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
