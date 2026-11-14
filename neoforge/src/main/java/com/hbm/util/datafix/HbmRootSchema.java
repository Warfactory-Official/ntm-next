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

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

final class HbmRootSchema extends Schema {

    HbmRootSchema(int versionKey) {
        super(versionKey, null);
    }

    @Override
    public void registerTypes(
            Schema schema,
            Map<String, Supplier<TypeTemplate>> entityTypes,
            Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        schema.registerType(true, HbmDataFixers.CHUNK, DSL::remainder);
        schema.registerType(false, HbmDataFixers.ENTITY_CHUNK, DSL::remainder);
        schema.registerType(false, HbmDataFixers.PLAYER, DSL::remainder);
        schema.registerType(false, HbmDataFixers.SEALED_ITEMS, DSL::remainder);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        return Map.of();
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        return Map.of();
    }
}
