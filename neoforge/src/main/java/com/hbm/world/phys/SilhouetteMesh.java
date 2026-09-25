// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.phys;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SilhouetteMesh {
    private static final char[] BOX_EDGES = {
        0, 1, 0, 2,
        1, 3, 0, 5,
        3, 2, 0, 3,
        2, 0, 0, 4,
        4, 6, 1, 4,
        6, 7, 1, 3,
        7, 5, 1, 5,
        5, 4, 1, 2,
        0, 4, 2, 4,
        5, 1, 2, 5,
        3, 7, 3, 5,
        6, 2, 3, 4
    };
    private static final char[] OCTAGONAL_PRISM_EDGES = {
        0, 1, 0, 2,
        1, 2, 0, 3,
        2, 3, 0, 4,
        3, 4, 0, 5,
        4, 5, 0, 6,
        5, 6, 0, 7,
        6, 7, 0, 8,
        7, 0, 0, 9,
        15, 14, 1, 8,
        14, 13, 1, 7,
        13, 12, 1, 6,
        12, 11, 1, 5,
        11, 10, 1, 4,
        10, 9, 1, 3,
        9, 8, 1, 2,
        8, 15, 1, 9,
        0, 8, 2, 9,
        9, 1, 2, 3,
        10, 2, 3, 4,
        11, 3, 4, 5,
        12, 4, 5, 6,
        13, 5, 6, 7,
        14, 6, 7, 8,
        15, 7, 8, 9
    };

    private final float[] vertices;
    private final float[] planes;
    private final char[] edges;

    private SilhouetteMesh(float[] vertices, float[] planes, char[] edges) {
        this.vertices = vertices;
        this.planes = planes;
        this.edges = edges;
    }

    public static SilhouetteMesh box(
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        assert minX < maxX && minY < maxY && minZ < maxZ;
        return new SilhouetteMesh(
                new float[] {
                    minX, minY, minZ, minX, minY, maxZ, minX, maxY, minZ, minX, maxY, maxZ,
                    maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, minZ, maxX, maxY, maxZ
                },
                new float[] {
                    -1F, 0F, 0F, minX, 1F, 0F, 0F, -maxX,
                    0F, -1F, 0F, minY, 0F, 1F, 0F, -maxY,
                    0F, 0F, -1F, minZ, 0F, 0F, 1F, -maxZ
                },
                BOX_EDGES);
    }

    public static SilhouetteMesh octagonalPrism(float[] vertices, float[] planes) {
        assert vertices.length == 48;
        assert planes.length == 40;
        return new SilhouetteMesh(vertices, planes, OCTAGONAL_PRISM_EDGES);
    }

    public VoxelShape at(double cameraX, double cameraY, double cameraZ) {
        return new Shape(cameraX, cameraY, cameraZ);
    }

    private final class Shape extends VoxelShape {
        private static final BitSetDiscreteVoxelShape EMPTY = new BitSetDiscreteVoxelShape(0, 0, 0);
        private static final DoubleList COORDINATES = DoubleArrayList.wrap(new double[] {0D});

        private final double cameraX;
        private final double cameraY;
        private final double cameraZ;

        private Shape(double cameraX, double cameraY, double cameraZ) {
            super(EMPTY);
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
        }

        @Override
        public DoubleList getCoords(Direction.Axis axis) {
            return COORDINATES;
        }

        @Override
        public void forAllEdges(Shapes.DoubleLineConsumer consumer) {
            for (int i = 0; i < edges.length; i += 4) {
                int face = edges[i + 2];
                boolean front = facesCamera(face);
                int otherFace = edges[i + 3];
                if (front == facesCamera(otherFace)) continue;
                int first = edges[i] * 3;
                int second = edges[i + 1] * 3;
                consumer.consume(
                        vertices[first],
                        vertices[first + 1],
                        vertices[first + 2],
                        vertices[second],
                        vertices[second + 1],
                        vertices[second + 2]);
            }
        }

        private boolean facesCamera(int face) {
            int at = face * 4;
            return planes[at] * cameraX
                            + planes[at + 1] * cameraY
                            + planes[at + 2] * cameraZ
                            + planes[at + 3]
                    >= 0D;
        }
    }
}
