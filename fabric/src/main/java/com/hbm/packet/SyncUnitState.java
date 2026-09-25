// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.network.VarLong;

public final class SyncUnitState {
    private final SyncUnitSchema schema;
    private final long allowed;
    private final boolean initialEffects;
    private Snapshot current;
    private boolean captured;

    private static final ClassValue<Boolean> INITIAL_EFFECTS =
            new ClassValue<>() {
                @Override
                protected Boolean computeValue(Class<?> type) {
                    try {
                        return type.getMethod("afterInitialSyncUnits").getDeclaringClass()
                                != SyncUnitSchema.class;
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

    public SyncUnitState(SyncUnitSchema schema) {
        this.schema = schema;
        allowed = schema.syncUnitMask();
        initialEffects = schema.syncEventUnits() != 0 || INITIAL_EFFECTS.get(schema.getClass());
    }

    public long publish(long units) {
        return publish(units, false);
    }

    public long publish(long units, boolean event) {
        return update(units, event, schema, 0);
    }

    public long ingest(
            long units, ByteBuf source, int start, int[] ends, boolean reset, long revision) {
        if (reset) {
            if (units != allowed)
                throw new IllegalArgumentException("Incomplete machine sync state");
            current = null;
        } else if (current == null) {
            throw new IllegalStateException("Partial machine sync state has no base");
        }
        captured = true;
        return update(units, false, new Received(source, start, ends), revision);
    }

    public boolean captured() {
        return captured;
    }

    private long update(long units, boolean event, SyncUnitSchema writer, long received) {
        if ((units & ~allowed) != 0)
            throw new IllegalArgumentException("Invalid machine sync unit");
        long events = event ? schema.syncEventUnits() : 0;
        units |= events;
        Snapshot previous = current;
        if (previous == null) units = allowed;
        else if (units == 0 && !event) return previous.revision;

        int count = Long.bitCount(allowed);
        long[] values = previous == null ? new long[count * 2] : previous.values;
        int[] lengths = previous == null ? new int[count] : previous.lengths;
        byte[][] fragments = previous == null ? null : previous.fragments;
        long changed = 0;
        ByteBuf output = SyncWire.SCRATCH.get();
        for (long remaining = units; remaining != 0; remaining &= remaining - 1) {
            long bit = Long.lowestOneBit(remaining);
            int unit = Long.numberOfTrailingZeros(bit);
            int index = Long.bitCount(allowed & (bit - 1));
            output.clear();
            writer.writeSyncUnit(unit, output);
            int length = output.writerIndex();
            long value = length <= Long.BYTES ? readInline(output, length) : 0;
            if (previous != null && (events & bit) == 0 && length == lengths[index]) {
                if (length <= Long.BYTES) {
                    if (value == values[index * 2]) continue;
                } else if (Arrays.equals(
                        fragments[index],
                        0,
                        length,
                        output.array(),
                        output.arrayOffset(),
                        output.arrayOffset() + length)) continue;
            }
            if (previous != null && changed == 0) values = values.clone();
            changed |= bit;
            values[index * 2] = value;
            if (length != lengths[index]) {
                if (previous != null && lengths == previous.lengths) lengths = lengths.clone();
                lengths[index] = length;
            }
            if (length > Long.BYTES || fragments != null && fragments[index] != null) {
                if (fragments == null) fragments = new byte[count][];
                else if (previous != null && fragments == previous.fragments)
                    fragments = fragments.clone();
                byte[] fragment = length > Long.BYTES ? new byte[length] : null;
                if (fragment != null) output.getBytes(0, fragment);
                fragments[index] = fragment;
            }
        }
        if (previous != null && changed == 0 && !event) return previous.revision;

        long revision = writer == schema ? SyncWire.nextRevision() : received;
        for (long remaining = changed; remaining != 0; remaining &= remaining - 1) {
            int index = Long.bitCount(allowed & (Long.lowestOneBit(remaining) - 1));
            values[index * 2 + 1] = revision;
        }
        current =
                new Snapshot(
                        allowed,
                        previous == null ? revision : previous.epoch,
                        revision,
                        previous == null ? 0 : previous.revision,
                        changed,
                        values,
                        lengths,
                        fragments,
                        initialEffects);
        return revision;
    }

    public Snapshot snapshot() {
        return current;
    }

    private static long readInline(ByteBuf input, int length) {
        return switch (length) {
            case 0 -> 0;
            case 1 -> input.getUnsignedByte(0);
            case 2 -> input.getUnsignedShort(0);
            case 3 -> input.getUnsignedMedium(0);
            case 4 -> input.getUnsignedInt(0);
            case 5 -> input.getUnsignedInt(0) << 8 | input.getUnsignedByte(4);
            case 6 -> input.getUnsignedInt(0) << 16 | input.getUnsignedShort(4);
            case 7 -> input.getUnsignedInt(0) << 24 | input.getUnsignedMedium(4);
            case 8 -> input.getLong(0);
            default -> throw new IllegalArgumentException();
        };
    }

    private static void writeInline(ByteBuf output, long value, int length) {
        switch (length) {
            case 0 -> {}
            case 1 -> output.writeByte((int) value);
            case 2 -> output.writeShort((int) value);
            case 3 -> output.writeMedium((int) value);
            case 4 -> output.writeInt((int) value);
            case 5 -> {
                output.writeInt((int) (value >>> 8));
                output.writeByte((int) value);
            }
            case 6 -> {
                output.writeInt((int) (value >>> 16));
                output.writeShort((int) value);
            }
            case 7 -> {
                output.writeInt((int) (value >>> 24));
                output.writeMedium((int) value);
            }
            case 8 -> output.writeLong(value);
            default -> throw new IllegalArgumentException();
        }
    }

    public static final class Snapshot {
        private final long allowed;
        private final long epoch;
        private final long revision;
        private final long previousRevision;
        private final long changed;
        private final long[] values;
        private final int[] lengths;
        private final byte[][] fragments;
        private final boolean initialEffects;

        private Snapshot(
                long allowed,
                long epoch,
                long revision,
                long previousRevision,
                long changed,
                long[] values,
                int[] lengths,
                byte[][] fragments,
                boolean initialEffects) {
            this.allowed = allowed;
            this.epoch = epoch;
            this.revision = revision;
            this.previousRevision = previousRevision;
            this.changed = changed;
            this.values = values;
            this.lengths = lengths;
            this.fragments = fragments;
            this.initialEffects = initialEffects;
        }

        public long revision() {
            return revision;
        }

        public long base(long requested) {
            return requested < epoch
                            || requested >= revision
                            || !initialEffects && selectedUnits(requested) == allowed
                    ? 0
                    : requested;
        }

        public long selectedUnits(long base) {
            if (base < epoch || base >= revision) return allowed;
            if (base == previousRevision) return changed;
            long selected = 0;
            int index = 0;
            for (long remaining = allowed; remaining != 0; remaining &= remaining - 1) {
                if (values[index * 2 + 1] > base) selected |= Long.lowestOneBit(remaining);
                index++;
            }
            return selected;
        }

        public int bodySize(long units) {
            assert (units & ~allowed) == 0;
            int size = VarLong.getByteSize(Long.compress(units, allowed));
            for (long remaining = units; remaining != 0; remaining &= remaining - 1) {
                int index = Long.bitCount(allowed & (Long.lowestOneBit(remaining) - 1));
                size = Math.addExact(size, lengths[index]);
            }
            return size;
        }

        public void writeBody(long units, ByteBuf output) {
            assert (units & ~allowed) == 0;
            VarLong.write(output, Long.compress(units, allowed));
            for (long remaining = units; remaining != 0; remaining &= remaining - 1) {
                int index = Long.bitCount(allowed & (Long.lowestOneBit(remaining) - 1));
                int length = lengths[index];
                if (length <= Long.BYTES) writeInline(output, values[index * 2], length);
                else output.writeBytes(fragments[index]);
            }
        }
    }

    private static final class Received implements SyncUnitSchema {
        private final ByteBuf source;
        private final int[] ends;
        private int from, slice;

        Received(ByteBuf source, int start, int[] ends) {
            this.source = source;
            this.ends = ends;
            from = start;
        }

        @Override
        public long syncUnitMask() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            int end = ends[slice++];
            output.writeBytes(source, from, end - from);
            from = end;
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            throw new UnsupportedOperationException();
        }
    }
}
