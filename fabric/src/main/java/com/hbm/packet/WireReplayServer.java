// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface WireReplayServer {

    boolean skipsViewerTicks();

    void resendWhenCaughtUp(BlockEntity entity);
}
