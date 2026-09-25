// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class CoolingTowerVisual
        implements EffectVisual<ParticleCoolingTower>, SimpleTickableVisual, ShaderLightVisual {
    private final ParticleCoolingTower particle;
    private final ParticleInstance instance;
    private final Vec3i origin;
    private @Nullable SectionCollector sections;
    private long sectionMin = Long.MIN_VALUE, sectionMax;

    public CoolingTowerVisual(VisualizationContext context, ParticleCoolingTower particle) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instance =
                context.instancerProvider()
                        .instancer(ParticleInstance.TYPE, ParticleModels.LIT)
                        .createInstance();
        writeTick();
    }

    @Override
    public void tick(Context context) {
        writeTick();
    }

    private void writeTick() {
        instance.setVisible(particle.isAlive());
        if (!particle.isAlive()) return;
        instance.previousPosition(
                (float) (particle.xo - origin.getX()),
                (float) (particle.yo - origin.getY()),
                (float) (particle.zo - origin.getZ()));
        instance.position(
                (float) (particle.x - origin.getX()),
                (float) (particle.y - origin.getY()),
                (float) (particle.z - origin.getZ()));
        instance.size(2F * particle.quadSize);
        instance.animation(particle.oRoll, particle.roll, particle.age, particle.getLifetime());
        instance.uvRegion(
                particle.visualU0(),
                particle.visualV0(),
                particle.visualU1() - particle.visualU0(),
                particle.visualV1() - particle.visualV0());
        instance.color(particle.rCol, particle.gCol, particle.bCol, particle.alpha)
                .light(0)
                .setChanged();
        publishLight();
    }

    private void publishLight() {
        if (sections == null) return;
        double radius = particle.quadSize * 1.415D + 1D;
        int x0 = Mth.floor(Math.min(particle.xo, particle.x) - radius) >> 4;
        int y0 = Mth.floor(Math.min(particle.yo, particle.y) - radius) >> 4;
        int z0 = Mth.floor(Math.min(particle.zo, particle.z) - radius) >> 4;
        int x1 = Mth.floor(Math.max(particle.xo, particle.x) + radius) >> 4;
        int y1 = Mth.floor(Math.max(particle.yo, particle.y) + radius) >> 4;
        int z1 = Mth.floor(Math.max(particle.zo, particle.z) + radius) >> 4;
        long min = SectionPos.asLong(x0, y0, z0), max = SectionPos.asLong(x1, y1, z1);
        if (min == sectionMin && max == sectionMax) return;
        sectionMin = min;
        sectionMax = max;
        var required = new LongOpenHashSet();
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) required.add(SectionPos.asLong(x, y, z));
        sections.sections(required);
    }

    @Override
    public void setSectionCollector(SectionCollector collector) {
        sections = collector;
        publishLight();
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        instance.delete();
    }
}
