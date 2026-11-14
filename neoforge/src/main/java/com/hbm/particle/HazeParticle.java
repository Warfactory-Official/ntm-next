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

import com.hbm.client.particle.VisualParticle;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class HazeParticle extends Particle implements VisualParticle {

    public static final ParticleRenderType HAZE_GROUP = new ParticleRenderType("hbm:haze", "HHZ");

    static final Identifier TEXTURE = RegistryUtil.id("textures/particle/haze.png");
    static final Quad[] QUADS = buildQuads();
    private @Nullable VisualizationManager visualManager;
    private static final float PARTICLE_SCALE = 10F;

    public HazeParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 600 + this.random.nextInt(100);
    }

    private static Quad[] buildQuads() {
        Random rand = new Random(50);
        Quad[] quads = new Quad[25];
        double walkX = 0, walkY = 0, walkZ = 0;
        for (int i = 0; i < quads.length; i++) {
            walkX += rand.nextGaussian() * 2.5D;
            walkY += rand.nextGaussian() * 0.15D;
            walkZ += rand.nextGaussian() * 2.5D;
            float sizeFactor = (float) (rand.nextDouble() * 0.25D + 0.75D);
            quads[i] =
                    new Quad(
                            (float) (walkX + rand.nextGaussian() * 0.5D),
                            (float) (walkY + rand.nextGaussian() * 0.5D),
                            (float) (walkZ + rand.nextGaussian() * 0.5D),
                            sizeFactor);
        }
        return quads;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) this.remove();

        int px = Mth.floor(this.x) + this.random.nextInt(15) - 7;
        int pz = Mth.floor(this.z) + this.random.nextInt(15) - 7;
        int py = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
        this.level.addParticle(
                ParticleTypes.LAVA,
                px + this.random.nextDouble(),
                py + 0.1D,
                pz + this.random.nextDouble(),
                0D,
                0D,
                0D);
    }

    float alpha() {
        return Math.max(0F, (float) Math.sin(this.age * Math.PI / 400D) * 0.25F) * 0.1F;
    }

    float scale() {
        return PARTICLE_SCALE;
    }

    double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    double interpZ(float pt) {
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
        return new HazeVisual(context, this);
    }

    @Override
    public ParticleRenderType getGroup() {
        return HAZE_GROUP;
    }

    record Quad(float offsetX, float offsetY, float offsetZ, float sizeFactor) {}
}
