// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.RandomSupport;

public final class WorldgenHash {

    private static final long X_MIX = 0x9E3779B97F4A7C15L;
    private static final long Y_MIX = 0xC2B2AE3D27D4EB4FL;
    private static final long Z_MIX = 0x165667B19E3779F9L;
    private static final long DRAW_MIX = 0xD6E8FEB86659FD93L;

    private WorldgenHash() {}

    public static long identifier(Identifier identifier) {
        RandomSupport.Seed128bit hash = RandomSupport.seedFromHashOf(identifier.toString());
        return mix(hash.seedLo() ^ Long.rotateLeft(hash.seedHi(), 29));
    }

    public static long worldDimensionSeed(long worldSeed, Identifier dimension) {
        return mix(worldSeed ^ identifier(dimension));
    }

    public static long domainSeed(long worldDimensionSeed, long domainSalt) {
        return mix(worldDimensionSeed ^ domainSalt);
    }

    public static long domainSeed(long worldSeed, Identifier dimension, Identifier domain) {
        return domainSeed(worldDimensionSeed(worldSeed, dimension), identifier(domain));
    }

    public static long position(long domainSeed, int x, int y, int z, int draw) {
        return mix(domainSeed ^ x * X_MIX ^ y * Y_MIX ^ z * Z_MIX ^ draw * DRAW_MIX);
    }

    public static float unitFloat(long hash) {
        return (hash >>> 40) * 0x1.0p-24F;
    }

    public static double unitDouble(long hash) {
        return (hash >>> 11) * 0x1.0p-53D;
    }

    public static int bounded(long hash, int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");

        long threshold = Long.remainderUnsigned(-(long) bound, bound);
        while (Long.compareUnsigned(hash, threshold) < 0) hash = mix(hash);
        return (int) Long.remainderUnsigned(hash, bound);
    }

    public static long mix(long value) {
        return RandomSupport.mixStafford13(value);
    }
}
