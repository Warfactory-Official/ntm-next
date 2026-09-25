// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IControlReceiver {

    void receiveControl(CompoundTag data);

    default void receiveControl(Player player, CompoundTag data) {
        receiveControl(data);
    }

    boolean hasPermission(Player player);
}
