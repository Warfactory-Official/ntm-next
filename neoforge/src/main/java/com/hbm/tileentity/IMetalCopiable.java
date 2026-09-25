// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.material.Mats;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface IMetalCopiable extends ICopiable {

    int[] getMatsToCopy();

    @Override
    default CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        if (getMatsToCopy().length > 0) tag.putIntArray("matFilter", getMatsToCopy());
        return tag;
    }

    @Override
    default String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        int[] ids = getMatsToCopy();
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++)
            names[i] = Mats.matById.get(ids[i]).getUnlocalizedName();
        return names;
    }

    @Override
    default void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {}
}
