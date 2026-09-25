// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.tileentity.BlockEntityBedrockOre;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.block.state.BlockState;

public final class BedrockOreBlockTintSource implements BlockTintSource {

    public static final BedrockOreBlockTintSource INSTANCE = new BedrockOreBlockTintSource();

    private BedrockOreBlockTintSource() {}

    @Override
    public int color(BlockState state) {
        return CommonColors.WHITE;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityBedrockOre ore
                ? ARGB.opaque(ore.tint)
                : CommonColors.WHITE;
    }
}
