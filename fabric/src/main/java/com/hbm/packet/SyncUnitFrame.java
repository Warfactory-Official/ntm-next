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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.VarLong;

public final class SyncUnitFrame {
    private static final ThreadLocal<int[]> ENDS =
            ThreadLocal.withInitial(() -> new int[Long.SIZE]);

    private SyncUnitFrame() {}

    public static void writeInitial(SyncUnitSchema schema, ByteBuf output) {
        long units = schema.syncUnitMask();
        VarLong.write(output, Long.compress(units, units));
        for (long remaining = units; remaining != 0; remaining &= remaining - 1) {
            schema.writeInitialSyncUnit(Long.numberOfTrailingZeros(remaining), output);
        }
    }

    public static long read(SyncUnitSchema schema, ByteBuf input) {
        long units = readUnits(schema, input, false);
        schema.afterSyncUnits(units);
        return units;
    }

    public static long readInitial(SyncUnitSchema schema, ByteBuf input) {
        long units = readUnits(schema, input, true);
        schema.afterInitialSyncUnits();
        return units;
    }

    public static long readUnits(SyncUnitSchema schema, ByteBuf input, boolean full) {
        long units = selection(schema, input, full);
        for (long remaining = units; remaining != 0; remaining &= remaining - 1) {
            schema.readSyncUnit(Long.numberOfTrailingZeros(remaining), input);
        }
        return units;
    }

    public static long readUnits(
            SyncUnitSchema schema,
            ByteBuf input,
            boolean full,
            SyncUnitState capture,
            long revision) {
        long units = selection(schema, input, full);
        int start = input.readerIndex();
        int[] ends = ENDS.get();
        int index = 0;
        for (long remaining = units; remaining != 0; remaining &= remaining - 1) {
            schema.readSyncUnit(Long.numberOfTrailingZeros(remaining), input);
            ends[index++] = input.readerIndex();
        }
        capture.ingest(units, input, start, ends, full, revision);
        return units;
    }

    private static long selection(SyncUnitSchema schema, ByteBuf input, boolean full) {
        long dense = VarLong.read(input);
        long allowed = schema.syncUnitMask();
        if ((dense & ~Long.compress(allowed, allowed)) != 0)
            throw new DecoderException("Invalid machine sync unit");
        long units = Long.expand(dense, allowed);
        if (full && units != allowed) throw new DecoderException("Incomplete machine sync state");
        return units;
    }
}
