// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.bomb.BlockEntityLaunchPadLarge;
import net.minecraft.world.entity.player.Inventory;

public class MenuLaunchPadLarge extends MenuLaunchPadBase<BlockEntityLaunchPadLarge> {

    public MenuLaunchPadLarge(int containerId, Inventory playerInv, BlockEntityLaunchPadLarge pad) {
        super(ModMenus.LAUNCH_PAD_LARGE.get(), containerId, playerInv, pad, pad);
    }
}
