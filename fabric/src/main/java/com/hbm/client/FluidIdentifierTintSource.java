// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class FluidIdentifierTintSource implements ItemTintSource {

    public static final FluidIdentifierTintSource INSTANCE = new FluidIdentifierTintSource();
    public static final MapCodec<FluidIdentifierTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private FluidIdentifierTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            FluidIdentifierData data =
                    stack.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            NTMFluidProperty prop = NTMFluidProperties.get(data.primary());
            if (prop != null) return prop.colorARGB();
        }
        return CommonColors.WHITE;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
