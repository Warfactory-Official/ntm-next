// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.CD_Gastank;
import com.hbm.items.machine.IFluidContainerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class GasTankTintSource implements ItemTintSource {

    public static final GasTankTintSource BOTTLE = new GasTankTintSource(false);
    public static final GasTankTintSource LABEL = new GasTankTintSource(true);
    public static final MapCodec<GasTankTintSource> BOTTLE_CODEC = MapCodec.unit(BOTTLE);
    public static final MapCodec<GasTankTintSource> LABEL_CODEC = MapCodec.unit(LABEL);

    private final boolean label;

    private GasTankTintSource(boolean label) {
        this.label = label;
    }

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (stack.getItem() instanceof IFluidContainerItem container) {
            CD_Gastank tank =
                    NTMFluidProperties.getTrait(
                            container.getContent(stack).type(), CD_Gastank.class);
            if (tank != null) return ARGB.opaque(label ? tank.labelColor : tank.bottleColor);
        }
        return CommonColors.WHITE;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return label ? LABEL_CODEC : BOTTLE_CODEC;
    }
}
