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

package com.hbm.particle;

public sealed interface RbmkJetShape {
    float uMin(int age);

    float uStep();

    float alpha(int age);

    float x0();

    float x1();

    float y0();

    float y1();

    record Flame(float scale, int maxAge) implements RbmkJetShape {
        private static final float U_STEP = 1F / 14F;

        @Override
        public float uMin(int age) {
            return age * 5 % 14 % 5 * U_STEP;
        }

        @Override
        public float uStep() {
            return U_STEP;
        }

        @Override
        public float alpha(int age) {
            float alpha = 1F;
            if (age < 20) alpha = age / 20F;
            if (age > maxAge - 20) alpha = (maxAge - age) / 20F;
            return alpha * 0.5F;
        }

        @Override
        public float x0() {
            return -scale - 1F;
        }

        @Override
        public float x1() {
            return scale - 1F;
        }

        @Override
        public float y0() {
            return -scale * 2F;
        }

        @Override
        public float y1() {
            return scale * 2F;
        }
    }

    record Steam(float scale, int maxAge) implements RbmkJetShape {

        public static final int LIFETIME = 10;
        private static final int COLUMNS = 20;
        private static final float U_STEP = 1F / COLUMNS;

        public Steam() {
            this(4F, LIFETIME);
        }

        @Override
        public float uMin(int age) {
            int texIndex = (int) ((double) age / (double) maxAge * COLUMNS) % COLUMNS - 1;
            if (texIndex < 0) texIndex += COLUMNS;
            return texIndex * U_STEP;
        }

        @Override
        public float uStep() {
            return U_STEP;
        }

        @Override
        public float alpha(int age) {
            return 0.25F;
        }

        @Override
        public float x0() {
            return scale * -0.25F - 0.9375F;
        }

        @Override
        public float x1() {
            return scale * 0.25F - 0.9375F;
        }

        @Override
        public float y0() {
            return -0.25F;
        }

        @Override
        public float y1() {
            return scale - 0.25F;
        }
    }
}
