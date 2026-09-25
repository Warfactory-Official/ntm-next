// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.block;

import net.minecraft.world.entity.Entity;

public interface IRadarCommandReceiver {

    boolean sendCommandPosition(int x, int y, int z);

    boolean sendCommandEntity(Entity target);
}
