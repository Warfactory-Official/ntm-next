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

import com.hbm.client.particle.MukeCloudVisual;
import com.hbm.client.particle.VisualParticle;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class MukeCloudParticle extends LegacyMoveParticle implements VisualParticle {

    public static final ParticleRenderType MUKE_CLOUD_GROUP =
            new ParticleRenderType("hbm:muke_cloud", "HMC");

    private static final Identifier TEXTURE = RegistryUtil.id("textures/particle/explosion.png");

    private final float friction;
    private @Nullable VisualizationManager visualManager;

    public MukeCloudParticle(
            ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        super(level, x, y, z);
        this.xd = mx;
        this.yd = my;
        this.zd = mz;

        if (yd > 0) {
            this.friction = 0.9F;

            if (yd > 0.1) this.lifetime = 92 + random.nextInt(11) + (int) (yd * 20);
            else this.lifetime = 72 + random.nextInt(11);

        } else if (yd == 0) {

            this.friction = 0.95F;
            this.lifetime = 52 + random.nextInt(11);

        } else {

            this.friction = 0.85F;
            this.lifetime = 122 + random.nextInt(31);
            this.age = 80;
        }
    }

    @Override
    public void tick() {
        this.hasPhysics = this.age > 2;

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime - 2) {
            this.remove();
            return;
        }

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= friction;
        this.yd *= friction;
        this.zd *= friction;

        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    public int texIndex(float partialTicks) {
        int clamped = Math.min(this.age, this.lifetime);
        return clamped * 25 / this.lifetime;
    }

    public Identifier texture() {
        return TEXTURE;
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
        return new MukeCloudVisual(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return MUKE_CLOUD_GROUP;
    }
}
