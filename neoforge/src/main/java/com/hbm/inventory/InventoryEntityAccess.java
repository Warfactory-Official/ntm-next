// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
