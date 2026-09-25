// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public record DirPos(@Nullable Direction dir, BlockPos pos) {

    public DirPos(int x, int y, int z, @Nullable Direction dir) {
        this(dir, new BlockPos(x, y, z));
    }

    public DirPos(BlockPos pos, @Nullable Direction dir) {
        this(dir, pos);
    }
}
