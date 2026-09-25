// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.tileentity.bomb.BlockEntityLaunchPad;
import net.minecraft.world.entity.player.Inventory;

public class MenuLaunchPad extends MenuLaunchPadBase<BlockEntityLaunchPad> {

    public MenuLaunchPad(int containerId, Inventory playerInv, BlockEntityLaunchPad pad) {
        super(ModMenus.LAUNCH_PAD.get(), containerId, playerInv, pad, pad);
    }
}
