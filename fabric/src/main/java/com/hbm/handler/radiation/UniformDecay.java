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

import com.hbm.lib.internal.VectorApi;
import com.hbm.lib.internal.VectorBackend;
import org.slf4j.LoggerFactory;

public interface UniformDecay {

    int CHANGED = 0;
    int ZEROED = 1;
    int FOG = 2;
    int DESTROY = 3;

    UniformDecay INSTANCE = choose();

    void decay(
            double[] uniform,
            int off,
            int n,
            double retention,
            double minBound,
            double fogRad,
            double destroyRad,
            long[] out);

    private static UniformDecay choose() {
        VectorBackend backend = VectorApi.backend();
        if (backend.arithmetic() && backend.masks()) {
            try {
                return new UniformDecayVector();
            } catch (Throwable t) {
                LoggerFactory.getLogger("NTM")
                        .warn("Vector uniform decay unavailable; using scalar", t);
            }
        }
        return new UniformDecayScalar();
    }
}
