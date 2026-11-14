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

package com.hbm.api.tile;

import org.jspecify.annotations.Nullable;

public enum HeatIntake {
    GAP,
    GAP_IN_ROOM,
    GAP_TO_ROOM,
    STORED;

    public int pull(@Nullable HeatHandler source, int heat, int ceiling, double diffusion) {
        if (source != null) {
            int stored = source.getHeatStored();
            if (this == STORED) {
                int draw = (int) (stored * diffusion);
                if (draw > 0) {
                    source.drainHeat(draw);
                    return heat + draw;
                }
            } else {
                int gap = stored - heat;
                if (gap == 0) return heat;
                if (this == GAP_IN_ROOM) gap = Math.min(gap, ceiling - heat);
                if (gap > 0) {
                    int draw = (int) Math.ceil(gap * diffusion);
                    if (this == GAP_TO_ROOM) draw = Math.min(draw, ceiling - heat);
                    source.drainHeat(draw);
                    return Math.min(heat + draw, ceiling);
                }
            }
        }
        return Math.max(heat - Math.max(heat / 1000, 1), 0);
    }
}
