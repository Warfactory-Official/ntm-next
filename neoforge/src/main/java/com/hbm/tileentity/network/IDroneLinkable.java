// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import net.minecraft.core.BlockPos;

public interface IDroneLinkable {

    BlockPos getPoint();

    void setNextTarget(BlockPos target);
}
