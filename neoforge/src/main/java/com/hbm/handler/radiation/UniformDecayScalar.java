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

package com.hbm.handler.radiation;

final class UniformDecayScalar implements UniformDecay {

    @Override
    public void decay(
            double[] uniform,
            int off,
            int n,
            double retention,
            double minBound,
            double fogRad,
            double destroyRad,
            long[] out) {
        long changed = 0L, zeroed = 0L, fog = 0L, destroy = 0L;
        for (int i = 0; i < n; i++) {
            double prev = uniform[off + i];
            double next = RadiationSystemNT.sanitize(prev * retention, minBound);
            if (next != prev) {
                uniform[off + i] = next;
                changed |= 1L << i;
            }
            if (next == 0.0D) {
                zeroed |= 1L << i;
            } else {
                if (next > fogRad) fog |= 1L << i;
                if (next >= destroyRad) destroy |= 1L << i;
            }
        }
        out[CHANGED] = changed;
        out[ZEROED] = zeroed;
        out[FOG] = fog;
        out[DESTROY] = destroy;
    }
}
