/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.items.machine;

import java.util.Locale;
import net.minecraft.world.item.Item;

public final class BreedingRodItem extends Item {

    public final Family family;
    public final BreedingRodType type;

    public BreedingRodItem(Item.Properties properties, Family family, BreedingRodType type) {
        super(properties);
        this.family = family;
        this.type = type;
    }

    public enum Family {
        SINGLE(1, 1),
        DUAL(2, 1),
        QUAD(4, 2);

        public final int channels;
        public final int radiationMultiplier;

        Family(int channels, int radiationMultiplier) {
            this.channels = channels;
            this.radiationMultiplier = radiationMultiplier;
        }
    }

    public enum BreedingRodType {
        LITHIUM(0D),
        TRITIUM(0.001D),
        CO(0D),
        CO60(30D),
        TH232(0.1D),
        THF(1.75D),
        U235(1D),
        NP237(2.5D),
        U238(0.25D),
        PU238(10D),
        PU239(5D),
        RGP(6.25D),
        WASTE(15D),
        LEAD(0D),
        URANIUM(0.35D),
        RA226(7.5D),
        AC227(30D);

        public static final BreedingRodType[] VALUES = values();

        public final double radiation;
        public final String id;

        BreedingRodType(double radiation) {
            this.radiation = radiation;
            this.id = name().toLowerCase(Locale.ROOT);
        }
    }
}
