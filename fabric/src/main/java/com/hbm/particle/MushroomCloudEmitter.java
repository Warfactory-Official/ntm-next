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
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.util.RandomSource;

public record MushroomCloudEmitter(
        double height,
        int skirt,
        int cap,
        double capSpread,
        double foldRadius2,
        double riseSpan,
        double riseDrop) {
    public static final MushroomCloudEmitter FULL =
            new MushroomCloudEmitter(1.8D, 100, 75, 0.5D, 1.5D, 3D, 1.5D);
    public static final MushroomCloudEmitter TINY =
            new MushroomCloudEmitter(1.6D, 50, 15, 0.2D, 0.75D, 2D, 1D);

    private static final double STEM_DRIFT = 0.05D;
    private static final double SKIRT_DRIFT = 0.5D;
    private static final double JITTER = 0.02D;

    @FunctionalInterface
    public interface Cloud {
        MukeCloudParticle make(
                ClientLevel level, double x, double y, double z, double mx, double my, double mz);
    }

    public void emit(
            ParticleEngine engine,
            ClientLevel level,
            RandomSource random,
            double x,
            double y,
            double z,
            Cloud cloud) {
        for (double rise = 0D; rise <= height; rise += 0.1D) {
            engine.add(
                    cloud.make(
                            level,
                            x,
                            y,
                            z,
                            random.nextGaussian() * STEM_DRIFT,
                            rise + random.nextGaussian() * JITTER,
                            random.nextGaussian() * STEM_DRIFT));
        }
        for (int n = skirt; n > 0; n--) {
            engine.add(
                    cloud.make(
                            level,
                            x,
                            y + 0.5D,
                            z,
                            random.nextGaussian() * SKIRT_DRIFT,
                            random.nextInt(5) == 0 ? JITTER : 0D,
                            random.nextGaussian() * SKIRT_DRIFT));
        }
        for (int n = cap; n > 0; n--) {
            double mx = random.nextGaussian() * capSpread;
            double mz = random.nextGaussian() * capSpread;
            double fold = mx * mx + mz * mz > foldRadius2 ? 0.5D : 1D;
            mx *= fold;
            mz *= fold;
            double lift =
                    (random.nextDouble() * riseSpan - riseDrop)
                            * (0.75D - (mx * mx + mz * mz))
                            * 0.5D;
            engine.add(
                    cloud.make(
                            level,
                            x,
                            y,
                            z,
                            mx,
                            height + lift + random.nextGaussian() * JITTER,
                            mz));
        }
    }
}
