/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: 2001 Robert Penner
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: BSD-3-Clause AND LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) 2001 Robert Penner. All rights reserved.
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

package com.hbm.render.anim;

final class AnimationEasing {

    private static final double BOUNCE = 7.5625;
    private static final double EXPO_FLOOR = 0.0009765625;
    private static final double EXPO_SCALE = 1.0009775161743164;

    private AnimationEasing() {}

    static double backOut(double t, double b, double c, double d, double overshoot) {
        double u = t / d - 1;
        return c * (u * u * ((overshoot + 1) * u + overshoot) + 1) + b;
    }

    static double bounceOut(double t, double b, double c, double d) {
        double u = t / d;
        if (u < 0.3636363744735718) return c * (BOUNCE * u * u) + b;
        if (u < 0.7272727489471436) {
            double w = u - 0.5454545617103577;
            return c * (BOUNCE * w * w + 0.75) + b;
        }
        if (u < 0.9090909361839294) {
            double w = u - 0.8181818127632141;
            return c * (BOUNCE * w * w + 0.9375) + b;
        }
        double w = u - 0.9545454382896423;
        return c * (BOUNCE * w * w + 0.984375) + b;
    }

    static double cubicIn(double t, double b, double c, double d) {
        double u = t / d;
        return c * u * u * u + b;
    }

    static double expoIn(double t, double b, double c, double d) {
        if (t == 0) return b;
        return c * (Math.pow(2, 10 * (t / d - 1)) - EXPO_FLOOR) * EXPO_SCALE + b;
    }

    static double quadIn(double t, double b, double c, double d) {
        double u = t / d;
        return c * u * u + b;
    }

    static double quadOut(double t, double b, double c, double d) {
        double u = t / d;
        return -c * u * (u - 2) + b;
    }

    static double quadInOut(double t, double b, double c, double d) {
        double u = t / (d / 2);
        if (u < 1) return c / 2 * u * u + b;
        double w = u - 1;
        return -c / 2 * (w * (w - 2) - 1) + b;
    }

    static double sineIn(double t, double b, double c, double d) {
        return -c * Math.cos(t / d * Math.PI / 2) + c + b;
    }

    static double sineOut(double t, double b, double c, double d) {
        return c * Math.sin(t / d * Math.PI / 2) + b;
    }
}
