// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.particle.QuadParticleVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import org.jspecify.annotations.Nullable;

public class ParticleDeadLeaf extends LegacyMoveParticle implements IQuadParticle, VisualParticle {

    public static final ParticleRenderType DEAD_LEAF_GROUP =
            new ParticleRenderType("hbm:dead_leaf", "HDL");

    private static int seedSeq;

    private final boolean flipU, flipV;
    private final float shade;
    private final float scale = 0.1F;
    private @Nullable VisualizationManager visualManager;

    public ParticleDeadLeaf(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.shade = 1F - this.random.nextFloat() * 0.2F;
        this.lifetime = 200 + this.random.nextInt(50);
        this.gravity = 0.2F;

        int id = seedSeq++;
        this.flipU = id % 2 == 0;
        this.flipV = id % 4 < 2;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.onGround) {
            this.xd += this.random.nextGaussian() * 0.002D;
            this.zd += this.random.nextGaussian() * 0.002D;

            if (this.yd < -0.025D) this.yd = -0.025D;
        }
    }

    @Override
    public float size(float pt) {
        return this.scale;
    }

    @Override
    public int argb(float pt) {
        int c = (int) (this.shade * 255F);
        return 0xFF000000 | c << 16 | c << 8 | c;
    }

    @Override
    public float u0() {
        return this.flipU ? 1F : 0F;
    }

    @Override
    public float u1() {
        return this.flipU ? 0F : 1F;
    }

    @Override
    public float v0() {
        return this.flipV ? 1F : 0F;
    }

    @Override
    public float v1() {
        return this.flipV ? 0F : 1F;
    }

    @Override
    public int light(float pt) {
        return getLightCoords(pt);
    }

    @Override
    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    @Override
    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    @Override
    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ClientLevel level() {
        return level;
    }

    @Override
    public @Nullable VisualizationManager visualManager() {
        return visualManager;
    }

    @Override
    public void visualManager(@Nullable VisualizationManager manager) {
        visualManager = manager;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext context, float partialTick) {
        return QuadParticleVisual.deadLeaf(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return DEAD_LEAF_GROUP;
    }
}
