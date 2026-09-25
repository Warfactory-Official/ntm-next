// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

public class BlockStalagmite extends BlockSpike {

    public static final MapCodec<BlockStalagmite> CODEC = simpleCodec(BlockStalagmite::new);

    public BlockStalagmite(Properties props) {
        super(props, false);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
