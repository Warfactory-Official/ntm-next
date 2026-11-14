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

package com.hbm.client.render;

public final class LPW2AnimationData {
    public static final int TRANSLATE = 0;
    public static final int CENTER_SPIN = 1;
    public static final int PISTON = 2;
    public static final int ENGINE = 3;
    public static final int FLAP = 4;
    public static final int STRETCH = 5;
    public static final int WIRE = 6;

    public static final int ZERO = 0;
    public static final int AXIAL = 1;
    public static final int HORIZONTAL = 2;
    public static final int VERTICAL = 3;
    public static final int COVER = 4;
    public static final int PISTON_WAVE = 5;
    public static final int ROTOR = 6;
    public static final int TURBINE = 7;
    public static final int SERVER_B = 8;
    public static final int SERVER_D = 9;

    public static final double HALF_PI = Math.PI / 2;
    public static final double HARMONIC_SECOND = 2;
    public static final double HARMONIC_FOURTH = 4;
    public static final double ROCK_FREQUENCY = 1.5;
    public static final double AXIAL_OFFSET = 2.23255;
    public static final double HALF = 0.5;
    public static final double ROCK_NORMALIZATION = 1.90596;
    public static final double ROTOR_NORMALIZATION = 25.1327412287;
    public static final double AXIAL_PERIOD = Math.PI * 4;
    public static final double ROCK_PERIOD = Math.PI * 4;
    public static final double PISTON_PERIOD = Math.PI * 2;
    public static final double ROTOR_PERIOD = Math.PI * 16;
    public static final double AXIAL_DIVISOR = 3;
    public static final double ROCK_DIVISOR = 5;
    public static final double SERVER_DIVISOR = 2;
    public static final double TURBINE_PERIOD = 100;
    public static final double TERMINAL_DIVISOR = 3;

    public static final String[] GROUPS = {
        "Center",
        "Rotor",
        "TurbineFront",
        "TurbineBack",
        "Piston",
        "Engine",
        "ShroudH",
        "ShroudV",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "SuspensionLeft",
        "SuspensionRight",
        "SuspensionTop",
        "SuspensionBottom",
        "WireLeft",
        "WireRight",
        "Cover",
        "SuspensionCoverFront",
        "SuspensionCoverBack",
        "SuspensionBackOuter",
        "SuspensionBackCenter",
        "Server1",
        "Server2",
        "Server3",
        "Server4",
        "Monitor",
        "Screen"
    };

    public static final int COLUMNS = 11;
    public static final int KIND = 0;
    public static final int SIGNAL_0 = 1;
    public static final int SIGNAL_1 = 2;
    public static final int DATA_0 = 3;
    public static final int DATA_1 = 7;
    public static final double[] PARTS = new double[GROUPS.length * COLUMNS];

    static {
        part(0, TRANSLATE, ZERO, AXIAL, 0, 0, -0.125, 0, 0, 0, 0, 0);
        part(1, CENTER_SPIN, ROTOR, ZERO, -0.125, -360, 3.5, 0, 0, 0, 0, 0);
        part(2, CENTER_SPIN, TURBINE, ZERO, -0.125, 360, 3.5, 0, 0, 0, 0, 0);
        part(3, CENTER_SPIN, TURBINE, ZERO, -0.125, -360, 3.5, 0, 0, 0, 0, 0);
        part(4, PISTON, ZERO, ZERO, -0.125, 0.375, 0.375, 0, 0, 0, 0, 0);
        part(5, ENGINE, ZERO, ZERO, -0.125, 3.5, 2.75, 2, 2, 0, 0, 0);
        part(6, TRANSLATE, HORIZONTAL, ZERO, 0, -0.125, 0, 0, 0, 0, 0, 0);
        part(7, TRANSLATE, VERTICAL, ZERO, 0.125, 0, 0, 0, 0, 0, 0, 0);
        part(8, FLAP, HORIZONTAL, VERTICAL, 0, -0.125, 112.5, 10, 5, 3.5, 6.96875, 8.5);
        part(9, FLAP, HORIZONTAL, VERTICAL, 0, -0.125, 67.5, 10, 5, 3.5, 6.96875, 8.5);
        part(10, FLAP, HORIZONTAL, VERTICAL, 0, -0.125, 292.5, 10, -5, 3.5, 6.96875, 8.5);
        part(11, FLAP, HORIZONTAL, VERTICAL, 0, -0.125, 247.5, 10, -5, 3.5, 6.96875, 8.5);
        part(12, FLAP, VERTICAL, HORIZONTAL, 0.125, 0, 22.5, 10, 5, 3.5, 6.96875, 8.5);
        part(13, FLAP, VERTICAL, HORIZONTAL, 0.125, 0, -22.5, 10, 5, 3.5, 6.96875, 8.5);
        part(14, FLAP, VERTICAL, HORIZONTAL, 0.125, 0, 202.5, 10, -5, 3.5, 6.96875, 8.5);
        part(15, FLAP, VERTICAL, HORIZONTAL, 0.125, 0, 157.5, 10, -5, 3.5, 6.96875, 8.5);
        part(16, STRETCH, VERTICAL, ZERO, -2.625, 0.6875, 0.125, 0, 0, 0, 0, 0);
        part(17, STRETCH, VERTICAL, ZERO, 2.625, 0.6875, -0.125, 0, 0, 0, 0, 0);
        part(18, STRETCH, HORIZONTAL, ZERO, 6.125, 0.6875, 0.125, 1, 0, 0, 0, 0);
        part(19, STRETCH, HORIZONTAL, ZERO, 0.875, 0.6875, -0.125, 1, 0, 0, 0, 0);
        part(20, WIRE, AXIAL, ZERO, 10, -2.9375, 0, 2.375, 0, 0, 0, 0);
        part(21, WIRE, AXIAL, ZERO, -10, 2.9375, 0, 2.375, 0, 0, 0, 0);
        part(22, TRANSLATE, ZERO, COVER, 0, 0, -0.125, 0, 0, 0, 0, 0);
        part(23, STRETCH, COVER, ZERO, 3.5, 3, 0.125, 2, 0, 0, 0, 0);
        part(24, STRETCH, COVER, ZERO, -5.5, 1.5, -0.125, 2, 0, 0, 0, 0);
        part(25, STRETCH, AXIAL, ZERO, -9, 1.25, -0.125, 2, 0, 0, 0, 0);
        part(26, STRETCH, AXIAL, ZERO, -9.5, 1.75, -0.125, 2, 0, 0, 0, 0);
        part(27, TRANSLATE, SERVER_B, SERVER_D, 0.015625, 0, 0.015625, 0, 0, 0, 0, 0);
        part(28, TRANSLATE, SERVER_D, SERVER_B, -0.015625, 0, 0.015625, 0, 0, 0, 0, 0);
        part(29, TRANSLATE, SERVER_D, SERVER_B, 0.015625, 0, -0.015625, 0, 0, 0, 0, 0);
        part(30, TRANSLATE, SERVER_B, SERVER_D, -0.015625, 0, -0.015625, 0, 0, 0, 0, 0);
        part(31, TRANSLATE, SERVER_D, SERVER_B, 0.015625, 0, 0.015625, 0, 0, 0, 0, 0);
        System.arraycopy(PARTS, 31 * COLUMNS, PARTS, 32 * COLUMNS, COLUMNS);
    }

    private LPW2AnimationData() {}

    private static void part(
            int slot,
            int kind,
            int signal0,
            int signal1,
            double a,
            double b,
            double c,
            double d,
            double e,
            double f,
            double g,
            double h) {
        int start = slot * COLUMNS;
        PARTS[start + KIND] = kind;
        PARTS[start + SIGNAL_0] = signal0;
        PARTS[start + SIGNAL_1] = signal1;
        PARTS[start + DATA_0] = a;
        PARTS[start + DATA_0 + 1] = b;
        PARTS[start + DATA_0 + 2] = c;
        PARTS[start + DATA_0 + 3] = d;
        PARTS[start + DATA_1] = e;
        PARTS[start + DATA_1 + 1] = f;
        PARTS[start + DATA_1 + 2] = g;
        PARTS[start + DATA_1 + 3] = h;
    }
}
