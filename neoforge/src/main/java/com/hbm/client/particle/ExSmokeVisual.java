// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.ParticleExSmoke;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Random;
import net.minecraft.core.Vec3i;

public final class ExSmokeVisual implements EffectVisual<ParticleExSmoke>, SimpleDynamicVisual {
    private static final int QUADS = 6;

    private final ParticleExSmoke particle;
    private final BillboardInstance[] instances = new BillboardInstance[QUADS];
    private final float[] offsetX = new float[QUADS];
    private final float[] offsetY = new float[QUADS];
    private final float[] offsetZ = new float[QUADS];
    private final float[] scale = new float[QUADS];
    private final float[] grey = new float[QUADS];
    private final Vec3i origin;

    public ExSmokeVisual(
            VisualizationContext context, ParticleExSmoke particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        var instancer =
                context.instancerProvider()
                        .instancer(InstanceTypes.BILLBOARD, ParticleModels.LIT_PARTICLE_BASE);
        Random random = new Random(particle.seed);
        for (int i = 0; i < QUADS; i++) {
            grey[i] = random.nextFloat() * 0.25F + 0.25F;
            scale[i] = random.nextFloat() + 0.5F;
            offsetX[i] = (float) ((random.nextGaussian() - 1D) * 0.75D);
            offsetY[i] = (float) ((random.nextGaussian() - 1D) * 0.75D);
            offsetZ[i] = (float) ((random.nextGaussian() - 1D) * 0.75D);
            instances[i] = instancer.createInstance();
            instances[i].size(2F * scale[i]).uvRegion(0F, 0F, 1F, 1F);
        }
        writeFrame(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        boolean visible = particle.isAlive();
        float x = (float) (particle.interpX(partialTick) - origin.getX());
        float y = (float) (particle.interpY(partialTick) - origin.getY());
        float z = (float) (particle.interpZ(partialTick) - origin.getZ());
        float alpha = particle.alpha(partialTick);
        int light = particle.light(partialTick);
        for (int i = 0; i < QUADS; i++) {
            BillboardInstance instance = instances[i];
            instance.setVisible(visible);
            if (!visible) continue;
            instance.position(x + offsetX[i], y + offsetY[i], z + offsetZ[i]);
            instance.color(grey[i], grey[i], grey[i], alpha);
            instance.light(light).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (BillboardInstance instance : instances) instance.delete();
    }
}
