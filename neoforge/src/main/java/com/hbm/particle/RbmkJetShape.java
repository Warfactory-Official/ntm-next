// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
