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
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

final class GunFluidKeyFix extends ItemComponentFix {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GUN = "hbm:gun_chemthrower";
    private static final String STATE = "hbm:gun_state";
    private static final String TYPE = "magtype0";

    GunFluidKeyFix(Schema outputSchema) {
        super(outputSchema, GUN::equals, STATE, GunFluidKeyFix::fixState, Traversal.OUTERMOST);
    }

    private static Dynamic<?> fixState(Dynamic<?> state) {
        if (state.getMapValues().result().isEmpty()) {
            LOGGER.warn("Gun state is not a compound; left unmigrated");
            return state;
        }
        var raw = state.get(TYPE).result().flatMap(type -> type.asNumber().result());
        if (raw.isEmpty()) return state;
        int id = raw.get().intValue();
        if (id < 0 || id >= BuiltInRegistries.FLUID.size()) {
            LOGGER.warn(
                    "Fluid magazine raw id {} names no registered fluid; migrated as the registry default",
                    id);
        }
        return state.set(
                TYPE,
                state.createString(
                        BuiltInRegistries.FLUID
                                .getKey(BuiltInRegistries.FLUID.byId(id))
                                .toString()));
    }
}
