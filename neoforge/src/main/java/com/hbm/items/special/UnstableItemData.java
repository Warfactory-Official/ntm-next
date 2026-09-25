// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStackTemplate;

public final class UnstableItemData {
    public static final long UNRESOLVED = Long.MIN_VALUE;

    private UnstableItemData() {}

    public static long cached(PatchedDataComponentMap components) {
        return __asm__(long) {
            aload components;
            getfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$unstableDeadline" "J";
        };
    }

    public static void cache(PatchedDataComponentMap components, long deadline) {
        __asm__ {
            aload components;
            lload deadline;
            putfield "net/minecraft/core/component/PatchedDataComponentMap" "hbm$unstableDeadline" "J";
        }
    }

    public static long cached(ItemStackTemplate template) {
        return __asm__(long) {
            aload template;
            getfield "net/minecraft/world/item/ItemStackTemplate" "hbm$unstableDeadline" "J";
        };
    }

    public static void cache(ItemStackTemplate template, long deadline) {
        __asm__ {
            aload template;
            lload deadline;
            putfield "net/minecraft/world/item/ItemStackTemplate" "hbm$unstableDeadline" "J";
        }
    }
}
