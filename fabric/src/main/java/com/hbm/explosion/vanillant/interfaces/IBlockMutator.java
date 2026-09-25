// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockMutator {

    void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos);

    void mutatePost(ExplosionVNT explosion, BlockPos pos);
}
