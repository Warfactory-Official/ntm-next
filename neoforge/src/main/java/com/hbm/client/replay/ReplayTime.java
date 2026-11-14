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

package com.hbm.client.replay;

import java.util.function.LongSupplier;
import org.jspecify.annotations.Nullable;

public final class ReplayTime {
    private static final ThreadLocal<PacketTime> PACKET_TIME =
            ThreadLocal.withInitial(PacketTime::new);
    private static @Nullable LongSupplier timeline;

    private ReplayTime() {}

    public static void open(LongSupplier clock) {
        timeline = clock;
    }

    public static void close(LongSupplier clock) {
        if (timeline == clock) timeline = null;
    }

    public static boolean active() {
        return timeline != null;
    }

    public static long now() {
        return timeline.getAsLong();
    }

    public static int decoded(int millis) {
        PACKET_TIME.get().decoded = millis;
        return millis;
    }

    public static long decodedMillis() {
        var time = PACKET_TIME.get();
        return time.applied != Long.MIN_VALUE ? time.applied : time.decoded;
    }

    public static long enter(long millis) {
        var time = PACKET_TIME.get();
        long previous = time.applied;
        time.applied = millis;
        return previous;
    }

    public static void leave(long previous) {
        PACKET_TIME.get().applied = previous;
    }

    public static long packetMillis() {
        if (!active()) return Long.MIN_VALUE;
        long millis = PACKET_TIME.get().applied;
        if (millis == Long.MIN_VALUE)
            throw new IllegalStateException("Replay door update has no packet timestamp");
        return millis;
    }

    private static final class PacketTime {
        long decoded = Long.MIN_VALUE;
        long applied = Long.MIN_VALUE;
    }
}
