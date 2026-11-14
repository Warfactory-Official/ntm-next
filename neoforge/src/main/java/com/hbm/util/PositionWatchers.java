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

package com.hbm.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;

public final class PositionWatchers<T> {

    private final Map<ServerLevel, Long2ObjectOpenHashMap<List<T>>> levels =
            new IdentityHashMap<>();

    public void clear() {
        levels.clear();
    }

    public void add(ServerLevel level, long key, T watcher) {
        List<T> watchers =
                levels.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(key, k -> new ArrayList<>(1));
        if (!watchers.contains(watcher)) watchers.add(watcher);
    }

    public void remove(ServerLevel level, long key, T watcher) {
        Long2ObjectOpenHashMap<List<T>> keys = levels.get(level);
        if (keys == null) return;
        List<T> watchers = keys.get(key);
        if (watchers == null || !watchers.remove(watcher)) return;
        if (watchers.isEmpty()) keys.remove(key);
        if (keys.isEmpty()) levels.remove(level);
    }

    public List<T> at(ServerLevel level, long key) {
        Long2ObjectOpenHashMap<List<T>> keys = levels.get(level);
        List<T> watchers = keys == null ? null : keys.get(key);
        return watchers == null ? List.of() : watchers;
    }
}
