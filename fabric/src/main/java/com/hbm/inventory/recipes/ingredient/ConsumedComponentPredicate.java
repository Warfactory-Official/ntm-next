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

package com.hbm.inventory.recipes.ingredient;

import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public record ConsumedComponentPredicate(DataComponentType<?> component)
        implements ComponentStatePredicate {
    public static final Codec<ConsumedComponentPredicate> CODEC =
            BuiltInRegistries.DATA_COMPONENT_TYPE
                    .byNameCodec()
                    .xmap(ConsumedComponentPredicate::new, ConsumedComponentPredicate::component);

    public static RegistryHandle<DataComponentPredicate.Type<ConsumedComponentPredicate>> TYPE;

    public static void register() {
        TYPE = Registrar.registerDataComponentPredicate("consume_component", CODEC);
    }

    @Override
    public boolean matches(DataComponentGetter components) {
        return true;
    }

    @Override
    public boolean claims(DataComponentType<?> type, ItemStack stack) {
        return type == component;
    }
}
