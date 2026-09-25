// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public abstract class SnapshotPeripheral<BE extends BlockEntity> implements IPeripheral {

    private static final VarHandle SEQ;
    private static final VarHandle LONGS = MethodHandles.arrayElementVarHandle(long[].class);
    private static final VarHandle REFS = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final int SPINS = 64;

    static {
        try {
            SEQ = MethodHandles.lookup().findVarHandle(SnapshotPeripheral.class, "seq", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final BE machine;
    private final String type;
    private final long[] longs;
    private final Object[] refs;
    private int seq;

    protected SnapshotPeripheral(BE machine, String type, int longs, int refs) {
        this.machine = machine;
        this.type = type;
        this.longs = new long[longs];
        this.refs = new Object[refs];
    }

    @FunctionalInterface
    protected interface Readout {
        Object[] read(SnapshotPeripheral<?> snapshot);
    }

    protected abstract void capture(BE machine);

    final void refresh() {
        int s = seq;
        SEQ.setOpaque(this, s + 1);
        VarHandle.storeStoreFence();
        try {
            capture(machine);
        } finally {

            SEQ.setRelease(this, s + 2);
        }
    }

    final BE target() {
        return machine;
    }

    protected final void put(int slot, long value) {
        longs[slot] = value;
    }

    protected final void put(int slot, double value) {
        longs[slot] = Double.doubleToRawLongBits(value);
    }

    protected final void put(int slot, boolean value) {
        longs[slot] = value ? 1L : 0L;
    }

    protected final void putRef(int slot, @Nullable Object value) {
        refs[slot] = value;
    }

    public final long longAt(int slot) {
        return (long) LONGS.getOpaque(longs, slot);
    }

    public final int intAt(int slot) {
        return (int) (long) LONGS.getOpaque(longs, slot);
    }

    public final double doubleAt(int slot) {
        return Double.longBitsToDouble((long) LONGS.getOpaque(longs, slot));
    }

    public final boolean booleanAt(int slot) {
        return (long) LONGS.getOpaque(longs, slot) != 0L;
    }

    public final @Nullable Object refAt(int slot) {
        return REFS.getOpaque(refs, slot);
    }

    protected final Object[] read(Readout readout) {
        for (int spins = 0; ; spins++) {
            int s = (int) SEQ.getAcquire(this);
            if ((s & 1) == 0) {
                Object[] out = readout.read(this);
                VarHandle.loadLoadFence();
                if ((int) SEQ.getAcquire(this) == s) return out;
            }
            if (spins < SPINS) {
                Thread.onSpinWait();
            } else {
                Thread.yield();
            }
        }
    }

    protected final BE machine() {
        assert machine.getLevel() != null
                        && machine.getLevel().getServer() != null
                        && machine.getLevel().getServer().isSameThread()
                : type + " touched its block entity off-thread";
        return machine;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final Object getTarget() {
        return machine;
    }

    @Override
    public final boolean equals(@Nullable IPeripheral other) {
        return other instanceof SnapshotPeripheral<?> peripheral
                && peripheral.machine == machine
                && peripheral.getClass() == getClass();
    }
}
