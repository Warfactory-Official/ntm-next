// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class PipeItemTintSource implements ItemTintSource {

    public static final PipeItemTintSource INSTANCE = new PipeItemTintSource();
    public static final MapCodec<PipeItemTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private PipeItemTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        return content == null
                ? FluidPipeTintData.DEFAULT_COLOR
                : FluidPipeTintData.colorFor(content.type());
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
