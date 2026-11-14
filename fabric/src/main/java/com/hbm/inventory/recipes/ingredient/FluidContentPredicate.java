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

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record FluidContentPredicate(Fluid fluid) implements ComponentStatePredicate {

    public static final Codec<FluidContentPredicate> CODEC =
            BuiltInRegistries.FLUID
                    .byNameCodec()
                    .xmap(FluidContentPredicate::new, FluidContentPredicate::fluid);

    public static RegistryHandle<DataComponentPredicate.Type<FluidContentPredicate>> TYPE;

    public static void register() {
        TYPE = Registrar.registerDataComponentPredicate("fluid_content", CODEC);
    }

    @Override
    public boolean matches(DataComponentGetter components) {
        FluidStackNTM content = components.get(ModDataComponents.FLUID_CONTENT.get());
        return (content == null ? Fluids.EMPTY : content.type()) == fluid;
    }

    @Override
    public boolean claims(DataComponentType<?> component, ItemStack stack) {
        return component == ModDataComponents.FLUID_CONTENT.get();
    }
}
