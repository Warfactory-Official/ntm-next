// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.blocks.fluid.AcidBlock;
import com.hbm.blocks.fluid.BlockCoriumFinite;
import com.hbm.blocks.fluid.BlockMud;
import com.hbm.blocks.fluid.BlockSchrabidic;
import com.hbm.blocks.fluid.BlockToxic;
import com.hbm.blocks.generic.BlockBarbedWire;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

final class WebCollision {

    private WebCollision() {}

    static boolean isWeb(BlockState state) {
        Block block = state.getBlock();
        return block instanceof WebBlock
                || block instanceof BlockBarbedWire
                || block instanceof AcidBlock
                || block instanceof BlockToxic
                || block instanceof BlockSchrabidic
                || block instanceof BlockMud
                || block instanceof BlockCoriumFinite;
    }
}
