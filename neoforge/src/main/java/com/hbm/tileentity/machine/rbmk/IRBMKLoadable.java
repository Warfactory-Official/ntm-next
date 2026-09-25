// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import net.minecraft.world.item.ItemStack;

public interface IRBMKLoadable {

    boolean canLoad(ItemStack toLoad);

    void load(ItemStack toLoad);

    boolean canUnload();

    ItemStack provideNext();

    void unload();
}
