// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.registration.FabricRegistrar;
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
        TYPE = FabricRegistrar.registerDataComponentPredicate("fluid_content", CODEC);
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
