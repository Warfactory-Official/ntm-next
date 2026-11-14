/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface SettingsHandler {
    @Nullable CompoundTag copySettings(Level level, BlockPos pos);

    void pasteSettings(CompoundTag settings, int option, Level level, Player player, BlockPos pos);

    default String getSettingsSourceId(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getDescriptionId();
    }

    default Component getSettingsSourceName(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getName();
    }

    default String @Nullable [] getSettingsOptions(Level level, BlockPos pos) {
        return null;
    }

    default boolean canCopySettings(Level level, BlockPos pos) {
        return true;
    }
}
