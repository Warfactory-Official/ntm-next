// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

public interface IMinecartRail {

    float railMaxSpeed();

    default void onMinecartPass(AbstractMinecart cart) {}
}
