// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class ItemSpawnEgg extends SpawnEggItem {

    public ItemSpawnEgg(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = getType(stack);
        return Component.translatable(
                getDescriptionId(), type == null ? "" : type.getDescription());
    }
}
