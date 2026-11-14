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

package com.hbm.capability.port;

import com.hbm.capability.port.ItemPort.SlotStack;
import java.util.IdentityHashMap;
import org.jspecify.annotations.Nullable;

public final class ItemPortCache {
    private final @Nullable SlotStack insert;
    private final @Nullable SlotStack extract;
    private final IdentityHashMap<int[], ItemPort> ports = new IdentityHashMap<>();

    public ItemPortCache(@Nullable SlotStack insert, @Nullable SlotStack extract) {
        this.insert = insert;
        this.extract = extract;
    }

    public ItemPort forSlots(int @Nullable [] slots) {
        if (slots == null) return ItemPort.none();
        var port = ports.get(slots);
        if (port == null) ports.put(slots, port = new ItemPort(slots, insert, extract));
        return port;
    }
}
