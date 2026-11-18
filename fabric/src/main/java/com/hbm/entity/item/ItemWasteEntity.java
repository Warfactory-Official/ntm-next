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

package com.hbm.entity.item;

import com.hbm.entity.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemWasteEntity extends ItemEntity {

    public ItemWasteEntity(EntityType<? extends ItemWasteEntity> type, Level level) {
        super(type, level);
        ((Entity) this).setPermanentlyInvulnerable(true);
        setUnlimitedLifetime();
    }

    public ItemWasteEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.WASTE_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }
}
