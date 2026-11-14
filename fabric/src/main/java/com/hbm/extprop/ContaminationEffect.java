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

package com.hbm.extprop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public final class ContaminationEffect {

    public static final Codec<ContaminationEffect> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .fieldOf("maxRad")
                                                    .forGetter(ContaminationEffect::peak),
                                            ExtraCodecs.POSITIVE_INT
                                                    .fieldOf("maxTime")
                                                    .forGetter(ContaminationEffect::lifetime),
                                            Codec.INT
                                                    .fieldOf("time")
                                                    .forGetter(ContaminationEffect::remaining),
                                            Codec.BOOL
                                                    .fieldOf("ignoreArmor")
                                                    .forGetter(ContaminationEffect::bypassesArmor))
                                    .apply(i, ContaminationEffect::new));

    private final double peak;
    private final int lifetime;
    private final boolean bypassesArmor;
    private int remaining;

    public ContaminationEffect(double peak, int lifetime, boolean bypassesArmor) {
        this(peak, lifetime, lifetime, bypassesArmor);
    }

    public ContaminationEffect(double peak, int lifetime, int remaining, boolean bypassesArmor) {
        if (lifetime <= 0) throw new IllegalArgumentException("lifetime " + lifetime);
        this.peak = peak;
        this.lifetime = lifetime;
        this.remaining = remaining;
        this.bypassesArmor = bypassesArmor;
    }

    public double peak() {
        return peak;
    }

    public int lifetime() {
        return lifetime;
    }

    public int remaining() {
        return remaining;
    }

    public boolean bypassesArmor() {
        return bypassesArmor;
    }

    public double getCurrentDose() {
        return peak * remaining / lifetime;
    }

    public void tick() {
        remaining--;
    }

    public boolean isExpired() {
        return remaining <= 0;
    }
}
