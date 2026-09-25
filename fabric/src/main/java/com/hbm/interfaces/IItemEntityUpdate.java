// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import net.minecraft.world.entity.item.ItemEntity;

public interface IItemEntityUpdate {

    boolean updateDroppedItem(ItemEntity entity);
}
