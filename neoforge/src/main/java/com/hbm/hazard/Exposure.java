/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum Exposure {
    GAS_LUNG("hazard.gasChlorine"),
    GAS_MONOXIDE("hazard.gasMonoxide"),
    GAS_INERT("hazard.gasInert"),
    PARTICLE_COARSE("hazard.particleCoarse"),
    PARTICLE_FINE("hazard.particleFine"),
    BACTERIA("hazard.bacteria"),
    GAS_BLISTERING("hazard.corrosive"),
    SAND("hazard.sand"),
    LIGHT("hazard.light");

    private static final Exposure[] ALL = values();

    public static final Codec<Exposure> CODEC =
            Codec.STRING.comapFlatMap(Exposure::byId, e -> e.id);

    public final String id;
    public final String translationKey;
    public final int bit;

    Exposure(String translationKey) {
        this.id = name().toLowerCase(Locale.ROOT);
        this.translationKey = translationKey;
        this.bit = 1 << ordinal();
    }

    private static DataResult<Exposure> byId(String id) {
        for (Exposure exposure : ALL)
            if (exposure.id.equals(id)) return DataResult.success(exposure);
        return DataResult.error(() -> "unknown hazard exposure '" + id + "'");
    }
}
