// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record ToolDig(
        ServerLevel level,
        ServerPlayer player,
        ItemStack held,
        ItemStack harvestTool,
        BlockPos reference,
        ToolPreset preset) {

    public ToolDig withHarvestTool(ItemStack harvestTool) {
        return new ToolDig(level, player, held, harvestTool, reference, preset);
    }
}
