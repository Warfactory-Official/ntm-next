// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.cart;

import com.hbm.entity.ModEntities;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import com.hbm.items.tool.ItemModMinecart.EnumMinecart;
import com.hbm.items.tool.ItemModMinecart;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityMinecartPowder extends EntityMinecartNTM {

    public EntityMinecartPowder(EntityType<? extends EntityMinecartPowder> type, Level level) {
        super(type, level);
    }

    public EntityMinecartPowder(Level level, double x, double y, double z, EnumCartBase base) {
        super(ModEntities.CART_POWDER.get(), level, x, y, z, base);
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(getBase(), EnumMinecart.POWDER);
    }
}
