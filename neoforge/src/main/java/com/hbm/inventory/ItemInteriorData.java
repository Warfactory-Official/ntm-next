// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
