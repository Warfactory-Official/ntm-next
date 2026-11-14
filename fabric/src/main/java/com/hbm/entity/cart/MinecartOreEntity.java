/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity.cart;

import com.hbm.entity.ModEntities;
import com.hbm.items.tool.ModMinecartItem.EnumCartBase;
import com.hbm.items.tool.ModMinecartItem.EnumMinecart;
import com.hbm.items.tool.ModMinecartItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MinecartOreEntity extends MinecartNTMEntity {

    public MinecartOreEntity(EntityType<? extends MinecartOreEntity> type, Level level) {
        super(type, level);
    }

    public MinecartOreEntity(Level level, double x, double y, double z, EnumCartBase base) {
        super(ModEntities.CART_ORE.get(), level, x, y, z, base);
    }

    @Override
    public ItemStack getCartItem() {
        return ModMinecartItem.createCartItem(getBase(), EnumMinecart.EMPTY);
    }
}
