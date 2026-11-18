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

package com.hbm.util.datafix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

final class UnpinnedLockFix extends EntityTagFix {

    private static final String LOCK_STATE = "hbm:lock_state";
    private static final String STACKLOCK = "hbm:stacklock";

    UnpinnedLockFix(Schema schema) {
        super(schema);
    }

    @Override
    protected Dynamic<?> fixEntity(Dynamic<?> tag) {
        if (tag.get("id").result().isEmpty()) return tag;
        var fixed =
                defaulted(tag)
                        ? tag.set("lockMod", tag.createDouble(0.1D))
                                .set("cheesable", tag.createBoolean(true))
                        : tag;
        var components = fixed.get("components").result();
        if (components.isEmpty()) return fixed;
        var map = components.get();
        boolean changed = false;
        if (map.get(LOCK_STATE).result().filter(UnpinnedLockFix::defaulted).isPresent()) {
            map = map.remove(LOCK_STATE);
            changed = true;
        }
        if (map.get(STACKLOCK).result().isPresent() && size(map) == 1) {
            map = map.remove(STACKLOCK);
            changed = true;
        }
        if (!changed) return fixed;
        return size(map) == 0 ? fixed.remove("components") : fixed.set("components", map);
    }

    private static boolean defaulted(Dynamic<?> lock) {
        return lock.get("lock").asInt(-1) == 0
                && !lock.get("isLocked").asBoolean(true)
                && lock.get("lockMod").asDouble(-1D) == 0D
                && !lock.get("cheesable").asBoolean(true);
    }

    private static int size(Dynamic<?> map) {
        return map.getMapValues().result().map(Map::size).orElse(0);
    }
}
