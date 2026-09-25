// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface MovedBlockEntityData {

    void hbm$captureFrom(BlockEntity source);

    void hbm$restoreCarried();
}
