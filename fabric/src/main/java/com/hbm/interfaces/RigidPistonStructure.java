// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public interface RigidPistonStructure {

    @Nullable BlockPos rigidStructureCore(Level level, BlockPos member);

    List<BlockPos> rigidStructureBlocks(Level level, BlockPos core);

    boolean isSameMultiblock(Block other);
}
