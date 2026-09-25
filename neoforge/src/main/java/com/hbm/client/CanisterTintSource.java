// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.CD_Canister;
import com.hbm.items.machine.IFluidContainerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class CanisterTintSource implements ItemTintSource {

    public static final CanisterTintSource INSTANCE = new CanisterTintSource();
    public static final MapCodec<CanisterTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private CanisterTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (stack.getItem() instanceof IFluidContainerItem container) {
            CD_Canister canister =
                    NTMFluidProperties.getTrait(
                            container.getContent(stack).type(), CD_Canister.class);
            if (canister != null) return ARGB.opaque(canister.color);
        }
        return CommonColors.WHITE;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
