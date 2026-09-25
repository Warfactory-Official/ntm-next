// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import net.minecraft.world.item.Item;

public class ItemGrenade extends Item {

    public final int fuse;

    public ItemGrenade(Properties properties, int fuse) {
        super(properties);
        this.fuse = fuse;
    }
}
