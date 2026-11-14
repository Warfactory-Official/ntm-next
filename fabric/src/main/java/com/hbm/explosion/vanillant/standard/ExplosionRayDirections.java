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

package com.hbm.explosion.vanillant.standard;

final class ExplosionRayDirections {
    static final double STEP = 0.3F;

    private static final ExplosionRayDirections[] CACHE = new ExplosionRayDirections[65];

    private final double[] steps;

    private ExplosionRayDirections(int resolution) {
        int edge = resolution - 1;
        steps =
                new double
                        [3
                                * (resolution * resolution * resolution
                                        - (edge - 1) * (edge - 1) * (edge - 1))];
        float span = (float) resolution - 1F;
        int n = 0;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    if (i != 0 && i != edge && j != 0 && j != edge && k != 0 && k != edge) continue;
                    double dx = (float) i / span * 2F - 1F;
                    double dy = (float) j / span * 2F - 1F;
                    double dz = (float) k / span * 2F - 1F;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    steps[n++] = dx / length * STEP;
                    steps[n++] = dy / length * STEP;
                    steps[n++] = dz / length * STEP;
                }
            }
        }
    }

    static double[] steps(int resolution) {
        if (resolution >= CACHE.length) return new ExplosionRayDirections(resolution).steps;
        var lattice = CACHE[resolution];
        if (lattice == null) CACHE[resolution] = lattice = new ExplosionRayDirections(resolution);
        return lattice.steps;
    }

    static int checkResolution(int resolution) {
        if (resolution < 2)
            throw new IllegalArgumentException("Ray lattice resolution " + resolution + " < 2");
        return resolution;
    }
}
