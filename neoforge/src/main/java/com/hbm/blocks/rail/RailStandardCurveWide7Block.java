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

package com.hbm.blocks.rail;

import com.hbm.blocks.rail.StandardCurveRailBlock;

public class RailStandardCurveWide7Block extends StandardCurveRailBlock {

    static final int[][] STAIRCASE_WIDE7 = {
        {1, 0, 0},
        {2, 0, 0},
        {0, 1, 1},
        {1, 1, 1},
        {2, 1, 1},
        {3, 1, 0},
        {4, 1, 0},
        {2, 2, 1},
        {3, 2, 0},
        {4, 2, 0},
        {5, 2, 0},
        {3, 3, 1},
        {4, 3, 0},
        {5, 3, 0},
        {4, 4, 1},
        {5, 4, 0},
        {6, 4, 0},
        {5, 5, 1},
        {5, 6, 1},
        {6, 5, 1},
        {6, 6, 1},
    };

    public RailStandardCurveWide7Block(Properties props) {
        super(props);
        this.width = 6;
    }

    @Override
    protected int[][] staircase() {
        return STAIRCASE_WIDE7;
    }
}
