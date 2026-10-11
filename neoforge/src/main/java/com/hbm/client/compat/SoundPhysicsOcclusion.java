// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.compat;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.sound.AudioDynamic;
import com.hbm.client.sound.MachineSoundInstance;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

public final class SoundPhysicsOcclusion {
    private static final boolean ENABLED = ModList.get().isLoaded("sound_physics_remastered");
    private static final Int2ObjectOpenHashMap<Emitter> SOURCES = new Int2ObjectOpenHashMap<>();
    private static final ThreadLocal<Emitter> ACTIVE = new ThreadLocal<>();

    private SoundPhysicsOcclusion() {}

    public static @Nullable Emitter capture(SoundInstance sound) {
        if (!ENABLED || sound.isRelative() || sound.getSource() != SoundSource.BLOCKS) return null;
        long owner =
                sound instanceof AudioDynamic dynamic
                        ? dynamic.machineCore()
                        : sound instanceof MachineSoundInstance machine
                                ? machine.machineCore
                                : MultiblockSurface.NO_CORE;
        if (!MultiblockSurface.hasCore(owner)) return null;
        Minecraft mc = Minecraft.getInstance();
        assert mc.isSameThread();
        var level = mc.level;
        if (level == null) return null;
        BlockPos core = BlockPos.of(owner);
        if (MultiblockSurface.foldedCore(level.getBlockState(core)) == null) return null;
        LongArrayList positions = new LongArrayList();
        for (BlockPos pos : MultiblockSurface.rigidStructureBlocks(level, core)) {
            if (core.equals(MultiblockSurface.coreOfAny(level, pos))) positions.add(pos.asLong());
        }
        long[] cells = positions.toLongArray();
        Arrays.sort(cells);
        BlockState[] states = new BlockState[cells.length];
        for (int i = 0; i < cells.length; i++)
            states[i] = level.getBlockState(BlockPos.of(cells[i]));
        return new Emitter(cells, states, sound.getX(), sound.getY(), sound.getZ());
    }

    public static void bind(int source, Emitter emitter) {
        emitter.thread = Thread.currentThread();
        synchronized (SOURCES) {
            SOURCES.put(source, emitter);
        }
    }

    public static void release(int source) {
        if (!ENABLED) return;
        synchronized (SOURCES) {
            SOURCES.remove(source);
        }
    }

    public static @Nullable Emitter begin(int source) {
        Emitter previous = ACTIVE.get();
        Emitter emitter;
        synchronized (SOURCES) {
            emitter = SOURCES.get(source);
        }
        ACTIVE.set(emitter != null && emitter.thread == Thread.currentThread() ? emitter : null);
        return previous;
    }

    public static void end(@Nullable Emitter previous) {
        ACTIVE.set(previous);
    }

    public static boolean isOrigin(Vec3 position) {
        Emitter emitter = ACTIVE.get();
        return emitter != null
                && position.x == emitter.x
                && position.y == emitter.y
                && position.z == emitter.z;
    }

    public static BlockHitResult initialRay(
            BlockGetter level,
            Vec3 from,
            Vec3 to,
            @Nullable BlockPos ignore,
            Operation<BlockHitResult> original) {
        Emitter emitter = ACTIVE.get();
        if (emitter == null) return original.call(level, from, to, ignore);
        boolean previous = emitter.initialRay;
        emitter.initialRay = true;
        try {
            return original.call(level, from, to, ignore);
        } finally {
            emitter.initialRay = previous;
        }
    }

    public static Object traverse(
            Vec3 from,
            Vec3 to,
            Object context,
            BiFunction<Object, BlockPos, Object> visit,
            Function<Object, Object> miss,
            Operation<Object> original) {
        Emitter emitter = ACTIVE.get();
        if (emitter == null || !emitter.initialRay)
            return original.call(from, to, context, visit, miss);
        BiFunction<Object, BlockPos, Object> previousVisit = emitter.visit;
        boolean previousEscaped = emitter.escaped;
        long previousStart = emitter.start;
        int previousStepX = emitter.stepX,
                previousStepY = emitter.stepY,
                previousStepZ = emitter.stepZ;
        emitter.visit = visit;
        emitter.start = BlockPos.asLong(Mth.floor(from.x), Mth.floor(from.y), Mth.floor(from.z));
        emitter.stepX = Mth.sign(to.x - from.x);
        emitter.stepY = Mth.sign(to.y - from.y);
        emitter.stepZ = Mth.sign(to.z - from.z);
        emitter.escaped = Arrays.binarySearch(emitter.cells, emitter.start) < 0;
        try {
            return original.call(from, to, context, emitter, miss);
        } finally {
            emitter.visit = previousVisit;
            emitter.escaped = previousEscaped;
            emitter.start = previousStart;
            emitter.stepX = previousStepX;
            emitter.stepY = previousStepY;
            emitter.stepZ = previousStepZ;
        }
    }

    public static final class Emitter implements BiFunction<Object, BlockPos, Object> {
        private final long[] cells;
        private final BlockState[] states;
        private final double x, y, z;
        private Thread thread;
        private boolean initialRay;
        private boolean escaped;
        private long start;
        private int stepX, stepY, stepZ;
        private BiFunction<Object, BlockPos, Object> visit;

        private Emitter(long[] cells, BlockState[] states, double x, double y, double z) {
            this.cells = cells;
            this.states = states;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public @Nullable Object apply(Object context, BlockPos pos) {
            if (!escaped) {
                int index = Arrays.binarySearch(cells, pos.asLong());
                if (index >= 0 && ((BlockGetter) context).getBlockState(pos) == states[index])
                    return null;
                if ((pos.getX() - BlockPos.getX(start)) * stepX >= 0
                        && (pos.getY() - BlockPos.getY(start)) * stepY >= 0
                        && (pos.getZ() - BlockPos.getZ(start)) * stepZ >= 0) escaped = true;
            }
            return visit.apply(context, pos);
        }
    }
}
