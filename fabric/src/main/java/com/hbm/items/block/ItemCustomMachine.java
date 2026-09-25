// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.items.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class ItemCustomMachine extends BlockItem {

    public ItemCustomMachine(Block block, Properties props) {
        super(block, props);
    }

    public static @Nullable ResourceKey<CustomMachineDefinition> typeOf(ItemStack stack) {
        return stack.get(ModDataComponents.CUSTOM_MACHINE_TYPE.get());
    }

    public static ItemStack of(Block block, ResourceKey<CustomMachineDefinition> type) {
        ItemStack stack = new ItemStack(block);
        stack.set(ModDataComponents.CUSTOM_MACHINE_TYPE.get(), type);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        CustomMachineDefinition definition = CustomMachineDefinitions.get(typeOf(stack));
        return definition != null
                ? definition.name()
                : Component.translatable("item.hbm.custom_machine.invalid");
    }
}
