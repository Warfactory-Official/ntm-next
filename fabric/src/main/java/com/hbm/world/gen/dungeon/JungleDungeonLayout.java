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

package com.hbm.world.gen.dungeon;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public final class JungleDungeonLayout {
    public static final int GRID = 25, PITCH = 4, WIDTH = 101, HEIGHT = 13;
    public static final int AIR = 1,
            BRICK = 2,
            CRACKED = 3,
            LAVA = 4,
            FRAGILE = 5,
            CRATE = 6,
            CIRCLE = 7,
            GLYPH = 8,
            TRAP = 24;
    private static final int CELLS = GRID * GRID;
    private static final int[] DX = {0, 0, -1, 1}, DZ = {-1, 1, 0, 0};

    private final byte[] families = new byte[3 * CELLS];
    private final byte[] doors = new byte[3 * CELLS];
    private final short[] order = new short[3 * CELLS];
    private final int[] counts = new int[3];
    private final byte[] blocks = new byte[WIDTH * WIDTH * HEIGHT];
    private int attempts;
    private int minX = WIDTH, minZ = WIDTH, maxX, maxZ;

    public JungleDungeonLayout(long seed) {
        Arrays.fill(families, (byte) -1);
        RandomSource random = RandomSource.create(seed);
        for (int floor = 0; floor < 3; floor++) compose(floor, random);
        for (int floor = 0; floor < 3; floor++) buildFloor(floor, random);
        for (int floor = 0; floor < 3; floor++) {
            for (int i = 0; i < counts[floor]; i++) {
                int cell = order[floor * CELLS + i];
                int family = families[floor * CELLS + cell];
                if (family == 1 || family == 2 || family == 7)
                    wallTraps(family, cell / GRID * PITCH, floor * PITCH, cell % GRID * PITCH);
            }
        }
    }

    private static int pickRoom(RandomSource random) {
        return Math.max(0, random.nextInt(25) - 9);
    }

    private void compose(int floor, RandomSource random) {
        int center = GRID / 2;
        putRoom(floor, center, center, 6, pickRoom(random));
        attempts = 0;
        addRoom(floor, center, center, 6, pickRoom(random), random);
    }

    private void putRoom(int floor, int x, int z, int door, int family) {
        int cell = x * GRID + z;
        families[floor * CELLS + cell] = (byte) family;
        doors[floor * CELLS + cell] = (byte) door;
        order[floor * CELLS + counts[floor]++] = (short) cell;
        minX = Math.min(minX, x * PITCH);
        minZ = Math.min(minZ, z * PITCH);
        maxX = Math.max(maxX, x * PITCH + 4);
        maxZ = Math.max(maxZ, z * PITCH + 4);
    }

    private void addRoom(int floor, int x, int z, int door, int family, RandomSource random) {
        if (++attempts > 700 || x < 0 || z < 0 || x >= GRID || z >= GRID) return;
        if (families[floor * CELLS + x * GRID + z] >= 0) {
            int direction = random.nextInt(4);
            addRoom(
                    floor,
                    x + DX[direction],
                    z + DZ[direction],
                    (direction ^ 1) + 2,
                    pickRoom(random),
                    random);
            return;
        }
        putRoom(floor, x, z, door, family);
        for (int branch = 0; branch < 6; branch++) {
            int direction = random.nextInt(4);
            addRoom(
                    floor,
                    x + DX[direction],
                    z + DZ[direction],
                    (direction ^ 1) + 2,
                    pickRoom(random),
                    random);
        }
    }

    private void buildFloor(int floor, RandomSource random) {
        boolean hasHole = false;
        int y = floor * PITCH;
        for (int i = 0; i < counts[floor]; i++) {
            int cell = order[floor * CELLS + i];
            int x = cell / GRID * PITCH, z = cell % GRID * PITCH;
            randomBox(x, y, z, 5, 1, 5, 2, random);
            box(x, y + 1, z, 5, 4, 5, AIR);
            randomBox(x, y + 4, z, 5, 1, 5, 2, random);
            int roll = random.nextInt(50);
            if (roll < 5) {
                randomBox(x + 1, y, z + 1, 3, 1, 3, 3, random);
            } else if (roll < 10) {
                set(x + 1 + random.nextInt(4), y + 1, z + random.nextInt(4), CRATE);
            } else if (roll < 20 && !hasHole) {
                for (int a = 1; a <= 3; a++) {
                    for (int b = 1; b <= 3; b++) {
                        if (get(x + a, y - 1, z + b) == AIR
                                && material(get(x + a, y - 4, z + b), true)) {
                            set(x + a, y, z + b, FRAGILE);
                            hasHole = true;
                        }
                    }
                }
            }
            decorate(families[floor * CELLS + cell], x, y, z, random);
            int door = doors[floor * CELLS + cell];
            for (int direction = 2; direction < 6; direction++) {
                boolean alongX = direction < 4;
                int wx = x + (direction == 5 ? 4 : 0), wz = z + (direction == 3 ? 4 : 0);
                randomBox(wx, y + 1, wz, alongX ? 5 : 1, 3, alongX ? 1 : 5, 116, random);
                if (direction == door)
                    box(
                            wx + (alongX ? 1 : 0),
                            y + 1,
                            wz + (alongX ? 0 : 1),
                            alongX ? 3 : 1,
                            3,
                            alongX ? 1 : 3,
                            AIR);
            }
        }
        if (!hasHole) {
            int selection = random.nextInt(counts[floor]);
            for (int cell = 0; cell < CELLS; cell++) {
                if (families[floor * CELLS + cell] >= 0 && selection-- == 0) {
                    set(cell / GRID * PITCH + 2, y, cell % GRID * PITCH + 2, CIRCLE);
                    break;
                }
            }
        }
    }

    private void decorate(int family, int x, int y, int z, RandomSource random) {
        switch (family) {
            case 3, 6 -> {
                for (int a = 1; a <= 3; a++) {
                    for (int b = 1; b <= 3; b++) {
                        if (a != 2 || b != 2)
                            trap(
                                    x + a,
                                    y + (family == 6 ? 4 : 0),
                                    z + b,
                                    family == 6 ? 7 : 1,
                                    true);
                    }
                }
            }
            case 4, 8 ->
                    trap(
                            x + 1 + random.nextInt(3),
                            y,
                            z + 1 + random.nextInt(3),
                            family == 4 ? 9 : 8,
                            false);
            case 5, 10, 13, 14 -> {
                for (int a = 1; a <= 3; a++) {
                    for (int b = 1; b <= 3; b++) {
                        if (random.nextInt(family == 5 || family == 14 ? 3 : 2) == 0)
                            trap(
                                    x + a,
                                    y,
                                    z + b,
                                    switch (family) {
                                        case 5 -> 4;
                                        case 10 -> 10;
                                        case 13 -> 11;
                                        default -> 5;
                                    },
                                    false);
                    }
                }
            }
            case 9, 11, 12, 15 ->
                    trap(
                            x + 2,
                            y + (family == 9 || family == 11 ? 4 : 0),
                            z + 2,
                            switch (family) {
                                case 9 -> 0;
                                case 11 -> 14;
                                case 12 -> 3;
                                default -> 13;
                            },
                            false);
            default -> {}
        }
    }

    private void wallTraps(int family, int x, int y, int z) {
        if (family == 7) {
            for (int i = 1; i <= 3; i++) trap(x, y + 2, z + i, 12, false);
            for (int i = 1; i <= 3; i++) trap(x + 4, y + 2, z + i, 12, false);
            for (int i = 1; i <= 3; i++) trap(x + i, y + 2, z, 12, false);
            for (int i = 1; i <= 3; i++) trap(x + i, y + 2, z + 4, 12, false);
        } else {
            int low = family == 1 ? 1 : 2, high = family == 1 ? 3 : 2, type = family == 1 ? 2 : 6;
            for (int i = low; i <= high; i++) trap(x + 2, y + i, z, type, false);
            for (int i = low; i <= high; i++) trap(x + 2, y + i, z + 4, type, false);
            for (int i = low; i <= high; i++) trap(x, y + i, z + 2, type, false);
            for (int i = low; i <= high; i++) trap(x + 4, y + i, z + 2, type, false);
        }
    }

    private static boolean material(int state, boolean includeTrap) {
        return state == BRICK || state == CRACKED || state == LAVA || includeTrap && state >= TRAP;
    }

    private void trap(int x, int y, int z, int type, boolean includeTrap) {
        if (material(get(x, y, z), includeTrap)) set(x, y, z, TRAP + type);
    }

    private void randomBox(
            int x, int y, int z, int sx, int sy, int sz, int paletteSize, RandomSource random) {
        for (int a = x; a < x + sx; a++) {
            for (int b = y; b < y + sy; b++) {
                for (int c = z; c < z + sz; c++) {
                    int roll = random.nextInt(paletteSize);
                    int state =
                            switch (paletteSize) {
                                case 2 -> BRICK + roll;
                                case 3 -> roll == 0 ? CRACKED : LAVA;
                                default -> roll < 100 ? BRICK + roll % 2 : GLYPH + roll - 100;
                            };
                    set(a, b, c, state);
                }
            }
        }
    }

    private void box(int x, int y, int z, int sx, int sy, int sz, int state) {
        for (int a = x; a < x + sx; a++)
            for (int b = y; b < y + sy; b++) for (int c = z; c < z + sz; c++) set(a, b, c, state);
    }

    private void set(int x, int y, int z, int state) {
        blocks[(x * HEIGHT + y) * WIDTH + z] = (byte) state;
    }

    public int get(int x, int y, int z) {
        return y < 0 ? 0 : blocks[(x * HEIGHT + y) * WIDTH + z];
    }

    public int roomCount(int floor) {
        return counts[floor];
    }

    public int roomCell(int floor, int index) {
        return order[floor * CELLS + index];
    }

    public int family(int floor, int cell) {
        return families[floor * CELLS + cell];
    }

    public int door(int floor, int cell) {
        return doors[floor * CELLS + cell];
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxZ() {
        return maxZ;
    }
}
