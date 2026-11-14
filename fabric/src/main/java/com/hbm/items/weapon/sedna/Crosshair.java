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

package com.hbm.items.weapon.sedna;

public enum Crosshair {
    NONE(0, 0, 0),
    CROSS(1, 55, 16),
    CIRCLE(19, 55, 16),
    SEMI(37, 55, 16),
    KRUCK(55, 55, 16),
    DUAL(1, 73, 16),
    SPLIT(19, 73, 16),
    CLASSIC(37, 73, 16),
    BOX(55, 73, 16),

    L_CROSS(0, 90, 32),
    L_KRUCK(32, 90, 32),
    L_CLASSIC(64, 90, 32),
    L_CIRCLE(96, 90, 32),
    L_SPLIT(0, 122, 32),
    L_ARROWS(32, 122, 32),
    L_BOX(64, 122, 32),
    L_CIRCUMFLEX(96, 122, 32),
    L_RAD(0, 154, 32),
    L_MODERN(32, 154, 32),
    L_BOX_OUTLINE(64, 154, 32);

    public int x;
    public int y;
    public int size;

    Crosshair(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }
}
