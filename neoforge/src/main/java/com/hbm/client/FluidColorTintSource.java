// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.IFluidContainerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class FluidColorTintSource implements ItemTintSource {

    public static final FluidColorTintSource INSTANCE = new FluidColorTintSource();
    public static final MapCodec<FluidColorTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private FluidColorTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {

        FluidStackNTM content =
                stack.getItem() instanceof IFluidContainerItem container
                        ? container.getContent(stack)
                        : stack.get(ModDataComponents.FLUID_CONTENT.get());
        NTMFluidProperty prop = content == null ? null : NTMFluidProperties.get(content.type());
        return prop != null ? prop.colorARGB() : CommonColors.WHITE;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
