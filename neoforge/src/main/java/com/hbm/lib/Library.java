// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import com.hbm.NuclearTech;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class Library {

    private Library() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NuclearTech.MOD_ID, path);
    }

    public static Identifier resolve(String reference) {
        return reference.indexOf(':') < 0 ? id(reference) : Identifier.parse(reference);
    }

    public static float roundFloat(double number, int decimal) {
        double pow = Math.pow(10, decimal);
        return (float) (Math.round(number * pow) / pow);
    }

    public static int blockPosToLocal(long serialized) {
        return ((int) (serialized & 0xFL) << 8)
                | (((int) (serialized >>> 12) & 0xF) << 4)
                | ((int) (serialized >>> 38) & 0xF);
    }

    public static int packLocal(int localX, int localY, int localZ) {
        return (localY << 8) | (localZ << 4) | localX;
    }

    public static int getLocalX(int packed) {
        return packed & 0xF;
    }

    public static int getLocalY(int packed) {
        return (packed >>> 8) & 0xF;
    }

    public static int getLocalZ(int packed) {
        return (packed >>> 4) & 0xF;
    }

    public static long sectionToLong(long ck, int subY) {
        return (ck << 42) | ((ck >>> 12) & 0x0000_03FF_FFF0_0000L) | (((long) subY) & 0xFFFFFL);
    }

    public static long setSectionY(long key, int subY) {
        return (key & ~0xFFFFFL) | (((long) subY) & 0xFFFFFL);
    }

    public static long shiftSectionX(long key, int dx) {
        return (key & ~(0x3FFFFFL << 42)) | (((key >>> 42) + dx & 0x3FFFFFL) << 42);
    }

    public static long shiftSectionZ(long key, int dz) {
        return (key & ~(0x3FFFFFL << 20)) | ((((key >>> 20) & 0x3FFFFFL) + dz & 0x3FFFFFL) << 20);
    }

    public static long sectionToChunkLong(long sck) {
        return (((sck << 22) >> 10) & 0xFFFF_FFFF_0000_0000L) | ((sck >> 42) & 0xFFFF_FFFFL);
    }

    public static List<int[]> getBlockPosInPath(int x, int y, int z, int length, Vec3 vec0) {
        List<int[]> list = new ArrayList<>();

        for (int i = 0; i <= length; i++) {
            list.add(new int[] {(int) (x + (vec0.x * i)), y, (int) (z + (vec0.z * i)), i});
        }

        return list;
    }
}
