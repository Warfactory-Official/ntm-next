// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

public final class HandPass {
    private HandPass() {}

    public static boolean local(ItemDisplayContext context, @Nullable ItemOwner owner) {
        return context.firstPerson() && owner != null && owner == Minecraft.getInstance().player;
    }
}
