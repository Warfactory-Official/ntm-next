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

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class InventorySlotBinding {
    private InventorySlotBinding() {}

    public static void bind(ItemStack stack, @Nullable Object source, int slot) {
        if (stack == ItemStack.EMPTY) return;
        PatchedDataComponentMap components = stack.components;
        __asm__ {
            aload components;
            aload source;
            putfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$inventorySource" "Ljava/lang/Object;";
            aload components;
            iload slot;
            putfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$inventorySlot" "I";
        }
    }

    public static void unbind(ItemStack stack, Object source) {
        if (stack == ItemStack.EMPTY) return;
        PatchedDataComponentMap components = stack.components;
        if (source(components) == source) bind(stack, null, 0);
    }

    public static void unbind(ItemStack stack, Object source, int slot) {
        if (stack == ItemStack.EMPTY) return;
        PatchedDataComponentMap components = stack.components;
        if (source(components) == source && slot(components) == slot) bind(stack, null, 0);
    }

    public static void changed(ItemStack stack) {
        changed(stack.components);
    }

    public static boolean isBound(ItemStack stack) {
        return source(stack.components) != null;
    }

    public static void changed(PatchedDataComponentMap components) {
        Object source = source(components);
        if (source == null) return;
        InventoryChanges.slotChanged(source, slot(components));
    }

    private static int slot(PatchedDataComponentMap components) {
        return __asm__(int) {
            aload components;
            getfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$inventorySlot" "I";
        };
    }

    private static Object source(PatchedDataComponentMap components) {
        return __asm__(Object) {
            aload components;
            getfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$inventorySource" "Ljava/lang/Object;";
        };
    }
}
