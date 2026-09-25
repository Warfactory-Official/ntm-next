// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.lib.Library;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

final class ProcessorRandom {

    static final long MAYBE = domain("maybe");
    static final long WEIGHTED = domain("weighted");
    static final long BOBBLE_TYPE = domain("bobble_type");
    static final long LOCK_PINS = domain("lock_pins");
    static final long PICK_ONE = domain("pick_one");
    static final long PICK_FALLBACK = domain("pick_fallback");
    static final long IMPACT_VILLAGE = domain("impact_village");
    static final long LOOT_PILE = domain("loot_pile");

    private ProcessorRandom() {}

    static float unitFloat(LevelReader level, BlockPos pos, long domain) {
        return WorldgenHash.unitFloat(at(level, pos.getX(), pos.getY(), pos.getZ(), domain, 0));
    }

    static int bounded(ServerLevelAccessor level, BlockPos pos, long domain, int draw, int bound) {
        return WorldgenHash.bounded(
                at(level, pos.getX(), pos.getY(), pos.getZ(), domain, draw), bound);
    }

    static int bounded(LevelReader level, BlockPos pos, long domain, int bound) {
        return WorldgenHash.bounded(
                at(level, pos.getX(), pos.getY(), pos.getZ(), domain, 0), bound);
    }

    static int bounded(LevelReader level, int x, int y, int z, long domain, int bound) {
        return WorldgenHash.bounded(at(level, x, y, z, domain, 0), bound);
    }

    static <T> T weighted(
            LevelReader level, int x, int y, int z, long domain, WeightedList<T> options) {
        int totalWeight = 0;
        for (Weighted<T> option : options.unwrap())
            totalWeight = Math.addExact(totalWeight, option.weight());
        int selection = bounded(level, x, y, z, domain, totalWeight);
        for (Weighted<T> option : options.unwrap()) {
            selection -= option.weight();
            if (selection < 0) return option.value();
        }
        throw new IllegalStateException("weighted selection exceeded its total");
    }

    static RandomSource source(LevelReader level, BlockPos pos, long domain) {
        return NtmWorldgenFields.get(server(level)).random(domain, pos);
    }

    static long worldSeed(LevelReader level) {
        return server(level).getLevel().getSeed();
    }

    private static long at(LevelReader level, int x, int y, int z, long domain, int draw) {
        return at(server(level), x, y, z, domain, draw);
    }

    private static ServerLevelAccessor server(LevelReader level) {

        if (!(level instanceof ServerLevelAccessor server)) {
            throw new IllegalStateException(
                    "a template processor drew without the placing server level: " + level);
        }
        return server;
    }

    private static long at(ServerLevelAccessor level, int x, int y, int z, long domain, int draw) {
        return WorldgenHash.position(
                NtmWorldgenFields.get(level).domainSeed(domain), x, y, z, draw);
    }

    private static long domain(String path) {
        return WorldgenHash.identifier(Library.id("processor/" + path));
    }
}
