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

package com.hbm.blocks.multiblock;

public enum GeometryCellPartition {
    STATIC(false, false, false),
    DOOR(true, false, false),
    DOOR_SEALED(true, true, false),
    RAIL(false, false, true);

    private final boolean opens;
    private final boolean seals;
    private final boolean waterlogs;

    GeometryCellPartition(boolean opens, boolean seals, boolean waterlogs) {
        this.opens = opens;
        this.seals = seals;
        this.waterlogs = waterlogs;
    }

    public static GeometryCellPartition of(boolean opens, boolean seals, boolean waterlogs) {
        for (GeometryCellPartition part : values()) {
            if (part.opens == opens && part.seals == seals && part.waterlogs == waterlogs)
                return part;
        }
        throw new IllegalStateException(
                "no geometry-cell partition for opens="
                        + opens
                        + " seals="
                        + seals
                        + " waterlogs="
                        + waterlogs
                        + "; add one with its own measured radix");
    }

    public boolean opens() {
        return opens;
    }

    public boolean seals() {
        return seals;
    }

    public boolean waterlogs() {
        return waterlogs;
    }
}
