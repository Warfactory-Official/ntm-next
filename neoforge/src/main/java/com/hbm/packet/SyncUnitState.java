/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.packet;

import com.hbm.packet.layout.UnitSyncLayout;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
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
            if (current != null) current.release();
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
        long events = 0;
        if (event) events = schema.syncEventUnits();
        else if (writer != schema) events = units & schema.syncEventUnits();
        boolean eventPublication = event || events != 0;
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
                        schema,
                        allowed,
                        previous == null ? revision : previous.epoch,
                        revision,
                        previous == null ? 0 : previous.revision,
                        changed,
                        values,
                        lengths,
                        fragments,
                        initialEffects,
                        eventPublication);
        if (previous != null) previous.release();
        return revision;
    }

    public Snapshot snapshot() {
        return current;
    }

    public void release() {
        if (current != null) current.release();
        current = null;
        captured = false;
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
        private final Object owner;
        private final long allowed;
        private final long epoch;
        private final long revision;
        private final long previousRevision;
        private final long changed;
        private final long[] values;
        private final int[] lengths;
        private final byte[][] fragments;
        private final boolean initialEffects;
        private final boolean event;
        private Long2LongOpenHashMap unitSelectionCache;
        private Long2ObjectOpenHashMap<EncodedPayload> frames;
        private long framePosition;
        private int frameBlockEntityType;
        private boolean frameIdentity;
        private int references = 1;

        private Snapshot(
                Object owner,
                long allowed,
                long epoch,
                long revision,
                long previousRevision,
                long changed,
                long[] values,
                int[] lengths,
                byte[][] fragments,
                boolean initialEffects,
                boolean event) {
            this.owner = owner;
            this.allowed = allowed;
            this.epoch = epoch;
            this.revision = revision;
            this.previousRevision = previousRevision;
            this.changed = changed;
            this.values = values;
            this.lengths = lengths;
            this.fragments = fragments;
            this.initialEffects = initialEffects;
            this.event = event;
        }

        public boolean event() {
            return event;
        }

        public long revision() {
            return revision;
        }

        public boolean ownedBy(Object owner) {
            return this.owner == owner;
        }

        public long base(long requested) {
            return requested < epoch
                            || requested >= revision
                            || !initialEffects && selectedUnits(requested) == allowed
                    ? 0
                    : requested;
        }

        public synchronized long selectedUnits(long base) {
            if (base < epoch || base >= revision) return allowed;
            if (base == previousRevision) return changed;
            if (unitSelectionCache != null && unitSelectionCache.containsKey(base))
                return unitSelectionCache.get(base);
            long selected = 0;
            int index = 0;
            for (long remaining = allowed; remaining != 0; remaining &= remaining - 1) {
                if (values[index * 2 + 1] > base) selected |= Long.lowestOneBit(remaining);
                index++;
            }
            if (unitSelectionCache == null) unitSelectionCache = new Long2LongOpenHashMap(4);
            unitSelectionCache.put(base, selected);
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

        public synchronized EncodedPayload retainFrame(
                long position, int blockEntityType, long base, long units) {
            if (references == 0) throw new IllegalStateException("Released machine sync snapshot");
            if (PacketWire.ccpPacketIdBytes() == null) {
                return UnitSyncLayout.encode(
                        BlockPos.of(position), blockEntityType, revision, base, this, units);
            }
            if (!frameIdentity) {
                frameIdentity = true;
                framePosition = position;
                frameBlockEntityType = blockEntityType;
            } else if (framePosition != position || frameBlockEntityType != blockEntityType) {
                throw new IllegalArgumentException(
                        "Machine sync snapshot reused for another block entity");
            }
            EncodedPayload frame = frames == null ? null : frames.get(base);
            if (frame == null) {
                frame =
                        UnitSyncLayout.encode(
                                BlockPos.of(position),
                                blockEntityType,
                                revision,
                                base,
                                this,
                                units);
                if (frames == null) frames = new Long2ObjectOpenHashMap<>(2);
                frames.put(base, frame);
            }
            frame.retain();
            return frame;
        }

        public synchronized void retain() {
            if (references == 0 || references == Integer.MAX_VALUE) {
                throw new IllegalStateException("Invalid machine sync snapshot retain");
            }
            references++;
        }

        public synchronized void release() {
            if (references == 0)
                throw new IllegalStateException("Machine sync snapshot already released");
            if (--references != 0 || frames == null) return;
            for (EncodedPayload frame : frames.values()) frame.release();
            frames.clear();
            frames = null;
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
