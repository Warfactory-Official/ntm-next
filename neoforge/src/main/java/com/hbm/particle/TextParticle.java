/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class TextParticle extends Particle {

    public static final ParticleRenderType TEXT_GROUP = new ParticleRenderType("hbm:text", "HTX");

    private final int color;
    private final String text;
    private float scale;

    public TextParticle(ClientLevel level, double x, double y, double z, int color, String text) {
        super(level, x, y, z);
        this.scale = (this.random.nextFloat() * 0.5F + 0.5F) * 2F;
        this.lifetime = 100;
        this.color = color;
        this.text = text;
        this.yd = 0.01D;
        this.hasPhysics = false;
    }

    public TextParticle setScale(float scale) {
        this.scale *= scale;
        return this;
    }

    public String text() {
        return this.text;
    }

    public int argb() {
        return 0xFF000000 | (this.color & 0xFFFFFF);
    }

    public boolean shadow() {
        return true;
    }

    public float renderScale() {
        return this.scale * 0.01F;
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
    public ParticleRenderType getGroup() {
        return TEXT_GROUP;
    }
}
