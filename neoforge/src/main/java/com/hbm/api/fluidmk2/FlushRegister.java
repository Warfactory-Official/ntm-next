// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.platform.Services;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.TickPhase;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FlushRegister {

    private static final FlushLane[] NO_LANES = new FlushLane[0];
    private static final BlockPos[] NO_POSITIONS = new BlockPos[0];
    private static final Direction[] NO_SIDES = new Direction[0];
    private static final int[] NO_ENDS = new int[0];
    private static final long[] NO_WORDS = new long[0];

    private static final ClassValue<Boolean> ADAPTED =
            new ClassValue<>() {
                @Override
                protected Boolean computeValue(Class<?> type) {
                    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                        for (Method m : c.getDeclaredMethods()) {
                            if (m.getName().equals("declareFlush")
                                    && m.getParameterCount() == 1
                                    && m.getParameterTypes()[0] == FlushLanes.class) {
                                return Boolean.FALSE;
                            }
                        }
                    }
                    return Boolean.TRUE;
                }
            };

    private final FluidFlushSender machine;
    private final List<IFluidHandlerMK2> gather = new ArrayList<>(4);

    private FlushLane[] lanes = NO_LANES;
    private BlockPos[] positions = NO_POSITIONS;
    private long[] contactKeys = NO_WORDS;
    private Direction[] sides = NO_SIDES;

    private int[] laneEnd = NO_ENDS;
    private long[] probed = NO_WORDS;
    private long[] live = NO_WORDS;
    private long[] parked = NO_WORDS;
    private long[] buckets = NO_WORDS;

    private boolean derived;
    private boolean inUse;

    public FlushRegister(FluidFlushSender machine) {
        this.machine = machine;
    }

    static boolean adaptsSendingTanks(Class<?> machine) {
        return ADAPTED.get(machine);
    }

    void expectLanes(int count) {
        if (derived && lanes.length != count) derived = false;
    }

    public void flush(ServerLevel level) {
        if (!derived) derive(level);
        if (positions.length == 0) return;

        long gameTime = level.getGameTime();
        BlockPos pos = machine.getBlockPos();

        if (!anyLaneDue(gameTime, pos)) return;
        probePending(level);

        int from = 0;
        for (int i = 0; i < lanes.length; i++) {
            int to = laneEnd[i];
            FlushLane lane = lanes[i];
            if (to > from
                    && TickPhase.every(gameTime, pos, lane.period())
                    && lane.enabled().getAsBoolean()) {
                FluidTankNTM tank = lane.tank();
                Fluid type = tank.getFluid();

                if (tank.getFill() > 0 && type != null && type != Fluids.EMPTY) {
                    serve(level, lane, type, tank.getPressure(), from, to);
                }
            }
            from = to;
        }
    }

    private boolean anyLaneDue(long gameTime, BlockPos pos) {
        int from = 0;
        for (int i = 0; i < lanes.length; i++) {
            int to = laneEnd[i];
            FlushLane lane = lanes[i];
            if (to > from
                    && TickPhase.every(gameTime, pos, lane.period())
                    && lane.enabled().getAsBoolean()
                    && lane.tank().getFill() > 0) {
                return true;
            }
            from = to;
        }
        return false;
    }

    public void invalidate() {
        derived = false;
    }

    void invalidateAt(long posKey) {
        if (posKey == machine.getBlockPos().asLong()) derived = false;
        for (int i = 0; i < contactKeys.length; i++) {
            if (contactKeys[i] != posKey) continue;
            probed[i >>> 6] &= ~(1L << i);
            parked[i >>> 6] &= ~(1L << i);
        }
    }

    void unpark() {
        Arrays.fill(parked, 0L);
    }

    long[] buckets() {
        return buckets;
    }

    void setBuckets(long[] keys) {
        buckets = keys;
    }

    void rememberBucket(long chunkKey) {
        for (long key : buckets) {
            if (key == chunkKey) return;
        }
        long[] grown = Arrays.copyOf(buckets, buckets.length + 1);
        grown[buckets.length] = chunkKey;
        buckets = grown;
    }

    long machineChunk() {
        return LevelNodeGraph.chunkOf(machine.getBlockPos().asLong());
    }

    private void derive(ServerLevel level) {
        FlushLanes declaration = new FlushLanes(machine);
        machine.declareFlush(declaration);
        List<FlushLane> declared = declaration.lanes();

        BlockPos pos = machine.getBlockPos();
        List<BlockPos> contactPositions = new ArrayList<>();
        List<Direction> contactSides = new ArrayList<>();
        int[] ends = new int[declared.size()];
        for (int i = 0; i < declared.size(); i++) {
            declared.get(i)
                    .faces()
                    .collect(
                            level,
                            pos,
                            (contact, side) -> {
                                contactPositions.add(contact);
                                contactSides.add(side);
                            });
            ends[i] = contactPositions.size();
        }

        lanes = declared.toArray(NO_LANES);
        positions = contactPositions.toArray(NO_POSITIONS);
        sides = contactSides.toArray(NO_SIDES);
        laneEnd = ends;
        contactKeys = new long[positions.length];
        for (int i = 0; i < positions.length; i++) contactKeys[i] = positions[i].asLong();
        int words = (positions.length + 63) >>> 6;
        probed = new long[words];
        live = new long[words];
        parked = new long[words];
        derived = true;
        FlushIndex.of(level).rebind(this, chunkKeys());
    }

    private long[] chunkKeys() {
        LongArrayList keys = new LongArrayList(4);
        keys.add(machineChunk());
        for (long contact : contactKeys) {
            long key = LevelNodeGraph.chunkOf(contact);
            if (!keys.contains(key)) keys.add(key);
        }
        return keys.toLongArray();
    }

    private void probePending(ServerLevel level) {
        int count = positions.length;
        for (int w = 0; w < probed.length; w++) {
            long todo = ~(probed[w] | parked[w]);
            while (todo != 0) {
                int i = (w << 6) + Long.numberOfTrailingZeros(todo);
                todo &= todo - 1;
                if (i >= count) return;
                probeContact(level, i);
            }
        }
    }

    private void probeContact(ServerLevel level, int i) {
        BlockPos at = positions[i];
        if (!LevelNodeGraph.resident(level, contactKeys[i])) {
            park(level, i, LevelNodeGraph.chunkOf(contactKeys[i]));
            return;
        }
        long blocker = MultiblockSurface.coreBlockingChunk(level, at);
        if (blocker != LevelNodeGraph.NO_BLOCKER) {
            park(level, i, blocker);
            return;
        }
        probed[i >>> 6] |= 1L << i;
        boolean present =
                Services.CAPS.find(FluidCaps.RECEIVER, level, at, FluidFace.any(sides[i])) != null;
        if (present) live[i >>> 6] |= 1L << i;
        else live[i >>> 6] &= ~(1L << i);
    }

    private void park(ServerLevel level, int i, long chunkKey) {
        parked[i >>> 6] |= 1L << i;
        FlushIndex.of(level).park(chunkKey, this);
    }

    private void serve(
            ServerLevel level, FlushLane lane, Fluid type, int pressure, int from, int to) {

        List<IFluidHandlerMK2> out = inUse ? new ArrayList<>(4) : gather;
        boolean pooled = out == gather;
        if (pooled) {
            gather.clear();
            inUse = true;
        }
        try {
            IFluidHandlerMK2 provider = lane.provider();
            for (int i = from; i < to; i++) {
                if ((live[i >>> 6] & (1L << i)) == 0) continue;
                IFluidHandlerMK2 receiver =
                        Services.CAPS.find(
                                FluidCaps.RECEIVER,
                                level,
                                positions[i],
                                FluidFace.of(sides[i], type));

                if (receiver == null || receiver == provider) continue;

                if (!containsIdentity(out, receiver)) out.add(receiver);
            }
            if (out.isEmpty()) return;
            FluidNetwork.provideDirect(provider, type, pressure, out);
        } finally {
            if (pooled) inUse = false;
        }
    }

    private static boolean containsIdentity(
            List<IFluidHandlerMK2> gather, IFluidHandlerMK2 receiver) {
        for (int i = 0, size = gather.size(); i < size; i++) {
            if (gather.get(i) == receiver) return true;
        }
        return false;
    }
}
