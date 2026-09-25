// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.conveyor;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface IConveyorPackage {

    List<ItemStack> getItemStacks();
}
