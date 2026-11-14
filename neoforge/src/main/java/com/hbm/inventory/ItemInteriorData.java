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

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class ItemInteriorData {
    private ItemInteriorData() {}

    public static Object[] cached(ItemInstance stack) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            return __asm__(Object[]) { aload components; getfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$interiorReferences" "[Ljava/lang/Object;"; };
        }
        if (stack instanceof ItemStackTemplate template) {
            return __asm__(Object[]) { aload template; getfield "net/minecraft/world/item/ItemStackTemplate" "hbm$interiorReferences" "[Ljava/lang/Object;"; };
        }
        return null;
    }

    public static void cache(ItemInstance stack, Object[] references) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            __asm__ { aload components; aload references; putfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$interiorReferences" "[Ljava/lang/Object;"; }
        } else if (stack instanceof ItemStackTemplate template) {
            __asm__ { aload template; aload references; putfield "net/minecraft/world/item/ItemStackTemplate" "hbm$interiorReferences" "[Ljava/lang/Object;"; }
        }
    }

    public static boolean opaque(ItemInstance stack) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            return __asm__(boolean) { aload components; getfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$opaqueReferences" "Z"; };
        }
        if (stack instanceof ItemStackTemplate template) {
            return __asm__(boolean) { aload template; getfield "net/minecraft/world/item/ItemStackTemplate" "hbm$opaqueReferences" "Z"; };
        }
        return false;
    }

    public static void opaque(ItemInstance stack, boolean opaque) {
        if (stack instanceof ItemStack live) {
            var components = live.components;
            __asm__ { aload components; iload opaque; putfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$opaqueReferences" "Z"; }
        } else if (stack instanceof ItemStackTemplate template) {
            __asm__ { aload template; iload opaque; putfield "net/minecraft/world/item/ItemStackTemplate" "hbm$opaqueReferences" "Z"; }
        }
    }
}
