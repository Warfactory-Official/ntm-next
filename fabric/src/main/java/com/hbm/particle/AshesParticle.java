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

import com.hbm.client.particle.AshesVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleTypes;
import org.jspecify.annotations.Nullable;

public class AshesParticle extends LegacyMoveParticle implements VisualParticle {

    public static final ParticleRenderType ASHES_GROUP = new ParticleRenderType("hbm:ashes", "HAS");

    private static final float FADE_TICKS = 40F;

    private static int seedSeq;

    private final int id = seedSeq++;
    private final float shade;
    private final float scale;
    private float tumble, tumbleO;
    private @Nullable VisualizationManager visualManager;

    public AshesParticle(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        lifetime = 1200 + random.nextInt(20);
        this.scale = scale * 0.9F + random.nextFloat() * 0.2F;
        shade = random.nextFloat() * 0.1F + 0.1F;
        gravity = 0.01F;
    }

    @Override
    public void tick() {
        drift(0.95D, 0.99D);
        boolean smoulders = id % 5 == 0;
        if (smoulders && onGround && random.nextInt(15) == 0)
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.125D, z, 0D, 0.05D, 0D);
    }

    @Override
    protected void turn() {
        tumbleO = tumble;
        if (!onGround) tumble += 2F * (id % 2 - 0.5F);
    }

    @Override
    protected void landed() {
        tumble = random.nextFloat() * 360F;
    }

    public boolean settled() {
        return onGround;
    }

    public float shade() {
        return shade;
    }

    public float settledY(float y) {
        return y - 0.09F;
    }

    public float size() {
        return scale;
    }

    public float spin(float pt) {
        return tumbleO + (tumble - tumbleO) * pt;
    }

    public float alpha(float pt) {
        return Math.min((lifetime - (age + pt)) / FADE_TICKS, 1F);
    }

    public int light(float pt) {
        return getLightCoords(pt);
    }

    public double interpX(float pt) {
        return xo + (x - xo) * pt;
    }

    public double interpY(float pt) {
        return yo + (y - yo) * pt;
    }

    public double interpZ(float pt) {
        return zo + (z - zo) * pt;
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
        return new AshesVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ASHES_GROUP;
    }
}
