// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlockLayering extends SnowLayerBlock {

    private final boolean alwaysReplaceable;

    public BlockLayering(Properties properties, boolean alwaysReplaceable) {
        super(properties.noOcclusion());
        this.alwaysReplaceable = alwaysReplaceable;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (alwaysReplaceable) return true;

        if (!context.getItemInHand().is(this.asItem())) return false;
        return super.canBeReplaced(state, context);
    }
}
