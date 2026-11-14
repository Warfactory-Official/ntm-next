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

package com.hbm.world.phys;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PlacementExtraShape extends VoxelShape {
    private static final BitSetDiscreteVoxelShape EMPTY = new BitSetDiscreteVoxelShape(0, 0, 0);
    private static final DoubleList COORDINATES = DoubleArrayList.wrap(new double[] {0D});

    private final double x0, y0, z0, x1, y1, z1;

    public PlacementExtraShape(double x0, double y0, double z0, double x1, double y1, double z1) {
        super(EMPTY);
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
    }

    @Override
    public DoubleList getCoords(Direction.Axis axis) {
        return COORDINATES;
    }

    @Override
    public void forAllEdges(Shapes.DoubleLineConsumer visitor) {
        edge(visitor, x0, y0, z0, x0, y0, z1);
        edge(visitor, x1, y0, z0, x1, y0, z1);
        edge(visitor, x0, y0, z0, x1, y0, z0);
        edge(visitor, x0, y0, z1, x1, y0, z1);
        edge(visitor, x0, y1, z0, x0, y1, z1);
        edge(visitor, x1, y1, z0, x1, y1, z1);
        edge(visitor, x0, y1, z0, x1, y1, z0);
        edge(visitor, x0, y1, z1, x1, y1, z1);
        edge(visitor, x0, y0, z0, x0, y1, z0);
        edge(visitor, x1, y0, z0, x1, y1, z0);
        edge(visitor, x0, y0, z1, x0, y1, z1);
        edge(visitor, x1, y0, z1, x1, y1, z1);
    }

    private static void edge(
            Shapes.DoubleLineConsumer visitor,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1) {
        if (x0 != x1 || y0 != y1 || z0 != z1) visitor.consume(x0, y0, z0, x1, y1, z1);
    }
}
