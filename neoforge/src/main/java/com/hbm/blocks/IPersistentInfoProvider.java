// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface IPersistentInfoProvider {

    static Component tankLine(FluidStackNTM stack, int capacity) {
        return Component.literal(
                        stack.amount()
                                + "/"
                                + capacity
                                + "mB "
                                + NTMFluidProperties.clientName(stack.type()))
                .withStyle(ChatFormatting.YELLOW);
    }

    void appendPersistentInfo(ItemStack stack, Consumer<Component> adder);
}
