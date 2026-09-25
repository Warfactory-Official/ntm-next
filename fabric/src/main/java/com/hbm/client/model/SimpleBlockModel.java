// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface SimpleBlockModel extends BlockModel<Void> {

    BlockStateModel.UnbakedRoot root(Block block, BlockState state);

    @Override
    default Void prepare() {
        return null;
    }

    @Override
    default BlockStateModel.UnbakedRoot root(Void prepared, Block block, BlockState state) {
        return root(block, state);
    }
}
