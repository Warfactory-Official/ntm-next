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

import com.hbm.client.particle.QuadParticleVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class BlackPowderSmokeParticle extends LegacyMoveParticle
        implements QuadParticle, VisualParticle {

    public static final ParticleRenderType BLACK_POWDER_SMOKE_GROUP =
            new ParticleRenderType("hbm:bp_smoke", "HBS");

    private static int seedSeq;

    private final int id;
    private final float hue;
    private final float scale;
    private float rotationPitch, prevRotationPitch;
    private @Nullable VisualizationManager visualManager;

    public BlackPowderSmokeParticle(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 30 + this.random.nextInt(15);
        this.scale = scale * 0.9F + this.random.nextFloat() * 0.2F;
        this.gravity = 0F;
        this.hue = 20F + this.random.nextFloat() * 20F;
        this.hasPhysics = false;
        this.id = seedSeq++;
    }

    @Override
    public void tick() {
        drift(0.65D, 0.65D);
    }

    @Override
    protected void turn() {
        prevRotationPitch = rotationPitch;
        rotationPitch += (1F - (float) age / lifetime) * 2F * ((id % 2) - 0.5F);
    }

    private float ageScaled(float pt) {
        return (this.age + pt) / (float) this.lifetime;
    }

    @Override
    public int argb(float pt) {
        float t = ageScaled(pt);
        float saturation = Math.max(1F - t * 4F, 0F);
        float brightness = Mth.clamp(1.25F - t * 2F, 0.7F, 1F);
        int rgb = Mth.hsvToRgb(this.hue / 255F, saturation, brightness) & 0xFFFFFF;
        float alpha = (float) Math.pow(1F - Math.min(t, 1F), 0.25) * 0.25F;
        return Mth.clamp((int) (alpha * 255F), 0, 255) << 24 | rgb;
    }

    @Override
    public float size(float pt) {
        return (float) ((0.25D + ageScaled(pt) + (this.age + pt) * 0.025D) * this.scale);
    }

    @Override
    public float spin(float pt) {
        return this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * pt;
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
        return QuadParticleVisual.particleBase(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return BLACK_POWDER_SMOKE_GROUP;
    }
}
