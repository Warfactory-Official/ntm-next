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

import com.hbm.client.particle.AmatFlashVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class AmatFlashParticle extends Particle implements VisualParticle {

    public static final ParticleRenderType AMAT_FLASH_GROUP =
            new ParticleRenderType("hbm:amat_flash", "HAF");

    public static final int SPIKES = 100;

    public static final float[] RIM = buildRim();

    private final float scale;
    private @Nullable VisualizationManager visualManager;

    public AmatFlashParticle(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 10;
        this.scale = scale;
    }

    public int light(float partialTicks) {
        return getLightCoords(partialTicks);
    }

    private static float[] buildRim() {
        Random rand = new Random(432L);
        Matrix4f pose = new Matrix4f();
        Vector3f corner = new Vector3f();
        float[] rim = new float[SPIKES * 3 * 3];

        for (int i = 0; i < SPIKES; i++) {
            pose.rotateX(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateY(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateZ(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateX(rand.nextFloat() * 360F * Mth.DEG_TO_RAD)
                    .rotateY(rand.nextFloat() * 360F * Mth.DEG_TO_RAD);

            float len = rand.nextFloat() * 20F + 15F;
            float width = rand.nextFloat() * 2F + 3F;

            put(rim, i, 0, pose, corner.set(-0.866F * width, len, -0.5F * width));
            put(rim, i, 1, pose, corner.set(0.866F * width, len, -0.5F * width));
            put(rim, i, 2, pose, corner.set(0F, len, width));
        }
        return rim;
    }

    private static void put(float[] rim, int spike, int corner, Matrix4f pose, Vector3f v) {
        pose.transformPosition(v);
        int base = (spike * 3 + corner) * 3;
        rim[base] = v.x;
        rim[base + 1] = v.y;
        rim[base + 2] = v.z;
    }

    public float extent(float partialTicks) {
        return 0.1F * this.scale * intensity(partialTicks);
    }

    public float intensity(float partialTicks) {
        return (this.age + partialTicks) / (float) this.lifetime;
    }

    public float coreAlpha(float partialTicks) {
        return 1F - intensity(partialTicks);
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
        return new AmatFlashVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return AMAT_FLASH_GROUP;
    }
}
