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

package com.hbm.blocks.bomb;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DigammaBounds {
    private static final int CHOICES = 9;
    private static final double PIXEL = 1.0 / 16.0;
    private static final DoubleList[] AXIS_PAIRS = prepareAxisPairs();
    private static final BitSetDiscreteVoxelShape CELL =
            BitSetDiscreteVoxelShape.withFilledBounds(1, 1, 1, 0, 0, 0, 1, 1, 1);

    private DigammaBounds() {}

    public static int sample() {
        var random = ThreadLocalRandom.current();
        int sample = 0;
        for (int drawIndex = 0; drawIndex < 6; drawIndex++) {
            int choice = random.nextInt(CHOICES);
            sample |= choice << (drawIndex * 4);
        }
        return sample;
    }

    public static double lower(int sample, int axis) {
        return choice(sample, axis) * PIXEL;
    }

    public static double upper(int sample, int axis) {
        return 1.0 - choice(sample, axis + 3) * PIXEL;
    }

    public static VoxelShape shape(int sample) {
        return new SampledShape(axisPair(sample, 0), axisPair(sample, 1), axisPair(sample, 2));
    }

    private static int choice(int sample, int drawIndex) {
        return (sample >>> (drawIndex * 4)) & 15;
    }

    private static DoubleList axisPair(int sample, int axis) {
        int lowerChoice = choice(sample, axis);
        int upperChoice = choice(sample, axis + 3);
        return AXIS_PAIRS[lowerChoice * CHOICES + upperChoice];
    }

    private static DoubleList[] prepareAxisPairs() {
        var pairs = new DoubleList[CHOICES * CHOICES];
        for (int lower = 0; lower < CHOICES; lower++) {
            for (int upper = 0; upper < CHOICES; upper++) {
                pairs[lower * CHOICES + upper] = DoubleList.of(lower * PIXEL, 1.0 - upper * PIXEL);
            }
        }
        return pairs;
    }

    private static final class SampledShape extends ArrayVoxelShape {
        private SampledShape(DoubleList x, DoubleList y, DoubleList z) {
            super(CELL, x, y, z);
        }

        @Override
        public void forAllEdges(Shapes.DoubleLineConsumer output) {
            super.forAllEdges(
                    (x0, y0, z0, x1, y1, z1) -> {
                        if (x0 != x1 || y0 != y1 || z0 != z1) {
                            output.consume(x0, y0, z0, x1, y1, z1);
                        }
                    });
        }
    }
}
