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

package com.hbm.client.model;

import it.unimi.dsi.fastutil.HashCommon;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.IntFunction;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

public abstract class ModelPartCache {
    private static final VarHandle PART =
            MethodHandles.arrayElementVarHandle(BlockStateModelPart[].class);
    private static final VarHandle HEAD = MethodHandles.arrayElementVarHandle(Entry[].class);
    final IntFunction<BlockStateModelPart> bake;
    final Object[] misses = new Object[32];

    private ModelPartCache(IntFunction<BlockStateModelPart> bake) {
        this.bake = bake;
        for (int i = 0; i < misses.length; i++) misses[i] = new Object();
    }

    static ModelPartCache dense(int size, IntFunction<BlockStateModelPart> bake) {
        return new Dense(size, bake);
    }

    static ModelPartCache bounded(int size, IntFunction<BlockStateModelPart> bake) {
        return new Bounded(size, bake);
    }

    public abstract BlockStateModelPart get(int key);

    public abstract int size();

    private static final class Dense extends ModelPartCache {
        private final BlockStateModelPart[] parts;

        Dense(int size, IntFunction<BlockStateModelPart> bake) {
            super(bake);
            parts = new BlockStateModelPart[size];
        }

        @Override
        public BlockStateModelPart get(int key) {
            var part = (BlockStateModelPart) PART.getAcquire(parts, key);
            if (part != null) return part;
            synchronized (misses[key & 31]) {
                part = (BlockStateModelPart) PART.getAcquire(parts, key);
                if (part == null) {
                    part = bake.apply(key);
                    PART.setRelease(parts, key, part);
                }
                return part;
            }
        }

        @Override
        public int size() {
            int count = 0;
            for (int i = 0; i < parts.length; i++) if (PART.getAcquire(parts, i) != null) count++;
            return count;
        }
    }

    private static final class Bounded extends ModelPartCache {
        private final Entry[] heads;
        private final Entry[] clock;
        private final Object publication = new Object();
        private volatile int size;
        private int hand;

        Bounded(int capacity, IntFunction<BlockStateModelPart> bake) {
            super(bake);
            heads = new Entry[HashCommon.nextPowerOfTwo(capacity * 2)];
            clock = new Entry[capacity];
        }

        private int bucket(int key) {
            return HashCommon.mix(key) & (heads.length - 1);
        }

        private BlockStateModelPart find(int key) {
            for (var entry = (Entry) HEAD.getAcquire(heads, bucket(key));
                    entry != null;
                    entry = entry.next) {
                if (entry.key == key) {
                    if (!entry.referenced) entry.referenced = true;
                    return entry.part;
                }
            }
            return null;
        }

        @Override
        public BlockStateModelPart get(int key) {
            var part = find(key);
            if (part != null) return part;
            synchronized (misses[HashCommon.mix(key) & 31]) {
                part = find(key);
                if (part != null) return part;
                part = bake.apply(key);
                synchronized (publication) {
                    if (size == clock.length) {
                        for (int visits = 0; visits < clock.length; visits++) {
                            var entry = clock[hand];
                            if (!entry.referenced) break;
                            entry.referenced = false;
                            hand = (hand + 1) % clock.length;
                        }
                        remove(clock[hand]);
                    } else size++;
                    int bucket = bucket(key);
                    var entry = new Entry(key, part, (Entry) HEAD.getAcquire(heads, bucket));
                    clock[hand] = entry;
                    hand = (hand + 1) % clock.length;
                    HEAD.setRelease(heads, bucket, entry);
                }
                return part;
            }
        }

        private void remove(Entry victim) {
            int bucket = bucket(victim.key);
            var head = (Entry) HEAD.getAcquire(heads, bucket);
            if (head == victim) HEAD.setRelease(heads, bucket, victim.next);
            else {
                var previous = head;
                while (previous.next != victim) previous = previous.next;
                previous.next = victim.next;
            }
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static final class Entry {
        final int key;
        final BlockStateModelPart part;
        volatile Entry next;
        volatile boolean referenced = true;

        Entry(int key, BlockStateModelPart part, Entry next) {
            this.key = key;
            this.part = part;
            this.next = next;
        }
    }
}
