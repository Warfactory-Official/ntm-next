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

package com.hbm.inventory.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum HazardSymbol {
    NONE(0, 0),
    RADIATION(195, 2),
    NOWATER(195, 63),
    ACID(195, 124),
    ASPHYXIANT(195, 185),
    CRYOGENIC(134, 185),
    ANTIMATTER(73, 185),
    OXIDIZER(12, 185);

    private static final HazardSymbol[] ALL = values();

    public static final Codec<HazardSymbol> CODEC =
            Codec.STRING.comapFlatMap(HazardSymbol::byName, Enum::name);

    public final int cellX;
    public final int cellY;

    HazardSymbol(int cellX, int cellY) {
        this.cellX = cellX;
        this.cellY = cellY;
    }

    private static DataResult<HazardSymbol> byName(String name) {
        for (HazardSymbol symbol : ALL)
            if (symbol.name().equals(name)) return DataResult.success(symbol);
        return DataResult.error(() -> "unknown hazard symbol '" + name + "'");
    }
}
