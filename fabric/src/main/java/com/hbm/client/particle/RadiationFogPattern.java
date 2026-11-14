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

package com.hbm.client.particle;

import java.util.Random;

final class RadiationFogPattern {

    static final int QUAD_COUNT = 25;
    static final double[] OFF_X = new double[QUAD_COUNT];
    static final double[] OFF_Y = new double[QUAD_COUNT];
    static final double[] OFF_Z = new double[QUAD_COUNT];
    static final double[] JIT_X = new double[QUAD_COUNT];
    static final double[] JIT_Y = new double[QUAD_COUNT];
    static final double[] JIT_Z = new double[QUAD_COUNT];
    static final float[] SIZE = new float[QUAD_COUNT];

    static {
        Random r = new Random(50L);
        double walkX = 0, walkY = 0, walkZ = 0;
        for (int i = 0; i < QUAD_COUNT; i++) {
            walkX += (r.nextGaussian() - 1.0) * 2.5;
            walkY += (r.nextGaussian() - 1.0) * 0.15;
            walkZ += (r.nextGaussian() - 1.0) * 2.5;
            OFF_X[i] = walkX;
            OFF_Y[i] = walkY;
            OFF_Z[i] = walkZ;
            SIZE[i] = (float) (r.nextDouble() * 7.5);
            JIT_X[i] = r.nextGaussian() * 0.5;
            JIT_Y[i] = r.nextGaussian() * 0.5;
            JIT_Z[i] = r.nextGaussian() * 0.5;
        }
    }

    private RadiationFogPattern() {}
}
