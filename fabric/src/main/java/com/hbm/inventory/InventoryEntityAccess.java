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

package com.hbm.inventory;

import com.hbm.interfaces.StoredItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;

public enum InventoryEntityAccess {
    NONE(false, false),
    EQUIPMENT(true, false),
    CARRIER(true, true),
    LEGACY(false, true),
    PLAYER(true, true),
    SINGLE(true, false);

    public final boolean indexed;
    public final boolean cargo;

    InventoryEntityAccess(boolean indexed, boolean cargo) {
        this.indexed = indexed;
        this.cargo = cargo;
    }

    private static final ClassValue<InventoryEntityAccess> ACCESS =
            new ClassValue<>() {
                @Override
                protected InventoryEntityAccess computeValue(Class<?> type) {
                    if (ServerPlayer.class.isAssignableFrom(type)) return PLAYER;
                    if (StoredItems.class.isAssignableFrom(type)) return LEGACY;
                    String name = type.getName();
                    boolean declaredCargo =
                            name.startsWith("net.minecraft.") || name.startsWith("com.hbm.");
                    if (LivingEntity.class.isAssignableFrom(type)) {
                        if (declaredCargo && Container.class.isAssignableFrom(type)) return LEGACY;
                        if (declaredCargo
                                && (InventoryCarrier.class.isAssignableFrom(type)
                                        || AbstractHorse.class.isAssignableFrom(type)))
                            return CARRIER;
                        return EQUIPMENT;
                    }
                    if (ItemEntity.class.isAssignableFrom(type)
                            || ItemFrame.class.isAssignableFrom(type)
                            || Display.ItemDisplay.class.isAssignableFrom(type)) return SINGLE;
                    if (declaredCargo && Container.class.isAssignableFrom(type)) return LEGACY;
                    return NONE;
                }
            };

    public static InventoryEntityAccess of(Entity entity) {
        return ACCESS.get(entity.getClass());
    }
}
