// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import net.minecraft.server.level.ServerPlayer;

public interface SyncAudience {
    boolean includes(ServerPlayer player);
}
