// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.extprop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class ContaminationEffect {

    public static final Codec<ContaminationEffect> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.DOUBLE.fieldOf("maxRad").forGetter(c -> c.maxRad),
                                            Codec.INT.fieldOf("maxTime").forGetter(c -> c.maxTime),
                                            Codec.INT.fieldOf("time").forGetter(c -> c.time),
                                            Codec.BOOL
                                                    .fieldOf("ignoreArmor")
                                                    .forGetter(c -> c.ignoreArmor))
                                    .apply(
                                            i,
                                            (maxRad, maxTime, time, ignoreArmor) -> {
                                                ContaminationEffect e =
                                                        new ContaminationEffect(
                                                                maxRad, maxTime, ignoreArmor);
                                                e.time = time;
                                                return e;
                                            }));
    public final double maxRad;
    public final int maxTime;
    public final boolean ignoreArmor;
    public int time;

    public ContaminationEffect(double maxRad, int maxTime, boolean ignoreArmor) {
        this.maxRad = maxRad;
        this.maxTime = maxTime;
        this.time = maxTime;
        this.ignoreArmor = ignoreArmor;
    }

    public double getRad() {
        return maxRad * ((double) time / (double) maxTime);
    }
}
