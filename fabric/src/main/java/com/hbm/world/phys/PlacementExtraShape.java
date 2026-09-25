// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
