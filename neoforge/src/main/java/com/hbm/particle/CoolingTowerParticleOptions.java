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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CoolingTowerParticleOptions(
        float baseScale,
        float maxScale,
        float lift,
        int life,
        float strafe,
        boolean windDir,
        float alphaMod,
        int color)
        implements ParticleOptions {

    public static final int NO_TINT = -1;

    public static MapCodec<CoolingTowerParticleOptions> codec(
            ParticleType<CoolingTowerParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(
                i ->
                        i.group(
                                        Codec.FLOAT
                                                .optionalFieldOf("base_scale", 1.0F)
                                                .forGetter(CoolingTowerParticleOptions::baseScale),
                                        Codec.FLOAT
                                                .optionalFieldOf("max_scale", 1.0F)
                                                .forGetter(CoolingTowerParticleOptions::maxScale),
                                        Codec.FLOAT
                                                .optionalFieldOf("lift", 0.3F)
                                                .forGetter(CoolingTowerParticleOptions::lift),
                                        Codec.INT
                                                .fieldOf("life")
                                                .forGetter(CoolingTowerParticleOptions::life),
                                        Codec.FLOAT
                                                .optionalFieldOf("strafe", 0.075F)
                                                .forGetter(CoolingTowerParticleOptions::strafe),
                                        Codec.BOOL
                                                .optionalFieldOf("wind_dir", true)
                                                .forGetter(CoolingTowerParticleOptions::windDir),
                                        Codec.FLOAT
                                                .optionalFieldOf("alpha_mod", 0.25F)
                                                .forGetter(CoolingTowerParticleOptions::alphaMod),
                                        Codec.INT
                                                .optionalFieldOf("color", NO_TINT)
                                                .forGetter(CoolingTowerParticleOptions::color))
                                .apply(i, CoolingTowerParticleOptions::new));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, CoolingTowerParticleOptions> streamCodec(
            ParticleType<CoolingTowerParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.FLOAT,
                CoolingTowerParticleOptions::baseScale,
                ByteBufCodecs.FLOAT,
                CoolingTowerParticleOptions::maxScale,
                ByteBufCodecs.FLOAT,
                CoolingTowerParticleOptions::lift,
                ByteBufCodecs.INT,
                CoolingTowerParticleOptions::life,
                ByteBufCodecs.FLOAT,
                CoolingTowerParticleOptions::strafe,
                ByteBufCodecs.BOOL,
                CoolingTowerParticleOptions::windDir,
                ByteBufCodecs.FLOAT,
                CoolingTowerParticleOptions::alphaMod,
                ByteBufCodecs.INT,
                CoolingTowerParticleOptions::color,
                CoolingTowerParticleOptions::new);
    }

    @Override
    public ParticleType<?> getType() {
        return HbmParticles.MIST_TOWER.get();
    }

    public static final class Builder {

        private float baseScale = 1.0F;
        private float maxScale = 1.0F;
        private float lift = 0.3F;
        private int life = -1;
        private float strafe = 0.075F;
        private boolean windDir = true;
        private float alphaMod = 0.25F;
        private int color = NO_TINT;

        public Builder setBaseScale(float f) {
            this.baseScale = f;
            return this;
        }

        public Builder setMaxScale(float f) {
            this.maxScale = f;
            return this;
        }

        public Builder setLift(float f) {
            this.lift = f;
            return this;
        }

        public Builder setLife(int i) {
            this.life = i;
            return this;
        }

        public Builder setStrafe(float f) {
            this.strafe = f;
            return this;
        }

        public Builder noWind() {
            this.windDir = false;
            return this;
        }

        public Builder alphaMod(float mod) {
            this.alphaMod = mod;
            return this;
        }

        public Builder setColor(int argb) {
            this.color = argb;
            return this;
        }

        public CoolingTowerParticleOptions build() {
            if (life < 0)
                throw new IllegalStateException("CoolingTowerParticleOptions requires a life");
            return new CoolingTowerParticleOptions(
                    baseScale, maxScale, lift, life, strafe, windDir, alphaMod, color);
        }
    }
}
