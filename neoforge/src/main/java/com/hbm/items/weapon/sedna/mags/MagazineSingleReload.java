// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class MagazineSingleReload extends MagazineSingleTypeBase {

    public MagazineSingleReload(int index, int capacity) {
        super(index, capacity);
    }

    @Override
    public MagazineSingleReload addConfigs(BulletConfig... cfgs) {
        super.addConfigs(cfgs);
        return this;
    }

    @Override
    public void reloadAction(ItemStack stack, Container inventory) {
        standardReload(stack, inventory, 1);
    }
}
