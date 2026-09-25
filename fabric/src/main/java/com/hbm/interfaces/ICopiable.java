// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface ICopiable {

    @Nullable CompoundTag getSettings(Level level, BlockPos pos);

    void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos);

    default String getSettingsSourceID(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getDescriptionId();
    }

    default Component getSettingsSourceDisplay(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getName();
    }

    default String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        return null;
    }

    default boolean copiable(Level level, BlockPos pos) {
        return true;
    }
}
