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

package com.hbm.handler.radio;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class RadioSystem {
    public static final String TEST_CHANNEL = "2012-08-06";

    private static final RadioSignal.Text[] MELODY =
            melody(
                    """
            0 4:6:0
            8 4:8:0-4:11:0-4:3:0
            16 4:8:0-4:11:0-4:3:0
            24 4:6:0
            32 4:10:0-4:1:0-4:5:0
            48 4:8:0
            56 4:11:0-4:3:0-4:6:1
            64 4:11:0-4:3:0-4:6:1
            72 4:6:0
            80 4:10:0-4:1:0-4:5:0
            96 4:6:0
            104 4:8:0-4:11:0-4:3:0
            112 4:8:0-4:11:0-4:3:0
            120 4:6:0
            128 4:10:0-4:1:0-4:5:0
            144 4:8:0
            152 4:8:0-4:11:0-4:3:0
            160 4:8:0-4:11:0-4:3:0
            168 4:6:0
            176 4:10:0-4:1:0-4:5:0
            192 4:6:0-0:8:0
            200 0:11:0
            208 4:8:0-4:11:0-4:3:0-0:8:1
            216 4:8:0-4:11:0-4:3:0
            224 4:6:0-0:8:0
            232 0:11:0
            240 4:10:0-4:1:0-4:5:0-0:8:1
            256 4:8:0-0:10:1
            264 4:11:0-4:3:0-4:6:1
            268 0:11:1
            272 4:11:0-4:3:0-4:6:1-0:10:1
            276 0:11:1
            280 4:6:0-0:10:1
            284 0:5:1
            288 4:10:0-4:1:0-4:5:0-0:3:1
            304 4:1:0-0:3:1
            312 4:8:0-4:11:0-4:5:1-0:8:0
            320 4:8:0-4:11:0-4:5:1-0:11:0
            324 0:1:1
            328 4:8:0-0:3:1
            336 4:6:0-4:11:0-4:3:0
            352 4:1:0-0:3:1
            360 4:8:0-4:11:0-4:5:1-0:8:0
            368 4:8:0-4:11:0-4:5:1-0:11:0
            372 4:5:1-0:1:1
            376 4:11:0-0:10:0
            384 4:6:0-4:11:0-4:3:0
            400 4:6:0-0:8:0
            404 0:11:0
            408 4:8:0-4:11:0-4:5:1-0:8:1
            416 4:8:0-4:11:0-4:5:1
            424 4:6:0-0:8:0
            428 0:11:0
            432 4:10:0-4:1:0-4:5:1-0:8:1
            448 4:8:0-0:10:1
            456 4:11:0-4:3:0-4:6:1
            460 0:11:1
            464 4:11:0-4:3:0-4:6:1-0:10:1
            468 0:11:1
            472 4:6:0-0:10:1
            476 0:6:1
            480 4:10:0-4:1:0-4:5:1-0:3:1
            496 4:1:0-0:3:1
            504 4:8:0-4:11:0-4:3:1-0:8:0
            512 4:8:0-4:11:0-4:3:1-0:11:0
            516 0:1:1
            520 4:11:0-0:3:1
            528 4:6:0-4:10:0-4:3:1-4:1:0
            536 0:3:1
            544 4:6:0-0:8:0
            552 4:8:0-4:11:0-4:3:1
            560 4:8:0-4:11:0-4:3:1
            """);

    private RadioSystem() {}

    public static final class Table {
        private final Object2ObjectOpenHashMap<String, RadioChannel> channels =
                new Object2ObjectOpenHashMap<>();
        private final ArrayList<RadioChannel> queue = new ArrayList<>();

        private RadioChannel channel(String name) {
            var channel = channels.get(name);
            if (channel == null) channels.put(name, channel = new RadioChannel());
            return channel;
        }
    }

    public static void send(Level level, String channel, String text) {
        send(level, channel, new RadioSignal.Text(text));
    }

    public static void send(Level level, String channel, char value) {
        send(level, channel, new RadioSignal.Char(value));
    }

    public static void send(Level level, String channel, int value) {
        send(level, channel, new RadioSignal.Int32(value));
    }

    public static void send(Level level, String channel, long value) {
        send(level, channel, new RadioSignal.Int64(value));
    }

    public static void send(Level level, String channel, double value) {
        send(level, channel, new RadioSignal.Decimal(value));
    }

    public static void send(Level level, String channel, RadioSignal signal) {
        var table = table(level);
        var target = table.channel(channel);
        var queued = target.queued;
        if (queued == null) {
            table.queue.add(target);
            target.queued = signal;
        } else if (integral(queued) && integral(signal)) {
            target.queued = new RadioSignal.Text(sum(integerValue(queued), integerValue(signal)));
        } else {
            target.queued = signal;
        }
    }

    public static @Nullable RadioChannel read(Level level, String channel) {
        var found = table(level).channels.get(channel);
        return found != null && found.readable() ? found : null;
    }

    public static void publish(MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            var table = ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$radio();
            long time = level.getGameTime();
            var queue = table.queue;
            for (int i = 0, size = queue.size(); i < size; i++) {
                var channel = queue.get(i);
                var signal = channel.queued;
                if (signal == null) continue;
                channel.queued = null;
                channel.publish(time, signal);
            }
            queue.clear();
            table.channel(TEST_CHANNEL).publish(time, testTransmission(time));
        }
    }

    public static RadioSignal.Text testTransmission(long gameTime) {
        return MELODY[(int) Math.floorMod(gameTime, MELODY.length)];
    }

    public static void seed(Level level, String channel, long stamp, RadioSignal signal) {
        table(level).channel(channel).publish(stamp, signal);
    }

    public static @Nullable RadioSignal queued(Level level, String channel) {
        var found = table(level).channels.get(channel);
        return found == null ? null : found.queued;
    }

    public static @Nullable RadioSignal unqueue(Level level, String channel) {
        var found = table(level).channels.get(channel);
        if (found == null) return null;
        var queued = found.queued;
        found.queued = null;
        return queued;
    }

    public static void forget(Level level, String channel) {
        var found = table(level).channels.remove(channel);
        if (found != null) found.queued = null;
    }

    private static Table table(Level level) {
        return ((com.hbm.interfaces.injected.ServerLevelExtension) (((ServerLevel) level)))
                .hbm$radio();
    }

    private static boolean integral(RadioSignal signal) {
        return switch (signal) {
            case RadioSignal.Int32 _,
                    RadioSignal.Int64 _ ->
                    true;
            case RadioSignal.Char character -> character.value() >= '0' && character.value() <= '9';
            case RadioSignal.Text text -> integerText(text.text());
            case RadioSignal.Decimal _ -> false;
        };
    }

    private static long integerValue(RadioSignal signal) {
        return switch (signal) {
            case RadioSignal.Int32 number -> number.value();
            case RadioSignal.Int64 number -> number.value();
            case RadioSignal.Char character -> character.value() - '0';
            case RadioSignal.Text text -> Long.parseLong(text.text());
            case RadioSignal.Decimal _ ->
                    throw new IllegalArgumentException("not an integer");
        };
    }

    static boolean integerText(String text) {
        int length = text.length();
        int start = length > 0 && (text.charAt(0) == '-' || text.charAt(0) == '+') ? 1 : 0;
        if (start == length) return false;
        char maxDigit = text.charAt(start) == '0' ? '7' : '9';
        long limit = text.charAt(0) == '-' ? Long.MIN_VALUE : -Long.MAX_VALUE;
        long floor = limit / 10;
        long negated = 0;
        for (int i = start; i < length; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > maxDigit) return false;
            int digit = c - '0';
            if (negated < floor) return false;
            negated *= 10;
            if (negated < limit + digit) return false;
            negated -= digit;
        }
        return true;
    }

    private static String sum(long a, long b) {
        long sum = a + b;
        if (((a ^ sum) & (b ^ sum)) < 0)
            return BigInteger.valueOf(a).add(BigInteger.valueOf(b)).toString();
        return Long.toString(sum);
    }

    private static RadioSignal.Text[] melody(String chords) {
        var melody = new RadioSignal.Text[640];
        Arrays.fill(melody, new RadioSignal.Text(""));
        for (var row : chords.split("\n")) {
            int split = row.indexOf(' ');
            melody[Integer.parseInt(row.substring(0, split))] =
                    new RadioSignal.Text(row.substring(split + 1));
        }
        return melody;
    }
}
