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

package com.hbm.blockentity;

import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface MaterialSettingsHandler extends SettingsHandler {

    int[] settingsMaterials();

    @Override
    default CompoundTag copySettings(Level level, BlockPos pos) {
        return writeMaterials(new CompoundTag(), settingsMaterials());
    }

    @Override
    default void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {}

    @Override
    default String[] getSettingsOptions(Level level, BlockPos pos) {
        return materialKeys(settingsMaterials());
    }

    static CompoundTag writeMaterials(CompoundTag settings, int[] ids) {
        if (ids.length > 0) settings.putIntArray("matFilter", ids);
        return settings;
    }

    static String[] materialKeys(int[] ids) {
        String[] keys = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            Material material = MaterialRegistry.byId(ids[i]);
            if (material == null) throw new IllegalStateException("no material with id " + ids[i]);
            keys[i] = material.translationKey();
        }
        return keys;
    }
}
