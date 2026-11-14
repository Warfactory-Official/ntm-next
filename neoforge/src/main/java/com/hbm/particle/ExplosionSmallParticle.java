/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.particle;

import com.hbm.client.particle.ExplosionSmallVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class ExplosionSmallParticle extends LegacyMoveParticle implements VisualParticle {

    public static final ParticleRenderType EXPLOSION_SMALL_GROUP =
            new ParticleRenderType("hbm:explosion_small", "HES");

    private static int spinSeq;

    public final float hue;
    public final float baseScale;
    private final float spinDir;
    public float roll, rollO;
    private @Nullable VisualizationManager visualManager;

    public ExplosionSmallParticle(
            ClientLevel level, double x, double y, double z, float scale, float speedMult) {
        super(level, x, y, z);
        this.lifetime = 25 + random.nextInt(10);
        this.baseScale = scale * 0.9F + random.nextFloat() * 0.2F;
        this.xd = random.nextGaussian() * speedMult;
        this.zd = random.nextGaussian() * speedMult;
        this.gravity = random.nextFloat() * -0.01F;
        this.hue = 20F + random.nextFloat() * 20F;
        this.spinDir = (spinSeq++ % 2) - 0.5F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        drift(0.65D, 1D);
    }

    @Override
    protected void turn() {
        rollO = roll;
        roll += (1 - (float) age / lifetime) * 5 * spinDir;
    }

    public int color(float partialTicks) {
        float ageScaled = (this.age + partialTicks) / this.lifetime;
        float sat = Math.max(1F - ageScaled * 2F, 0F);
        float val = Mth.clamp(1.25F - ageScaled * 2F, this.hue * 0.01F - 0.1F, 1F);
        return Mth.hsvToRgb(this.hue / 255F, sat, val);
    }

    public float alpha(float partialTicks) {
        float ageScaled = (this.age + partialTicks) / this.lifetime;
        return (float) Math.pow(1 - Math.min(ageScaled, 1F), 0.25) * 0.5F;
    }

    public float renderScale(float partialTicks) {
        double ageScaled = (double) (this.age + partialTicks) / this.lifetime;
        return (float)
                ((0.25 + 1 - Math.pow(1 - ageScaled, 4) + (this.age + partialTicks) * 0.02)
                        * this.baseScale);
    }

    public float roll(float partialTicks) {
        return this.rollO + (this.roll - this.rollO) * partialTicks;
    }

    public int light(float partialTicks) {
        return getLightCoords(partialTicks);
    }

    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

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
        return new ExplosionSmallVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return EXPLOSION_SMALL_GROUP;
    }
}
