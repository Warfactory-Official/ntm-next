// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import net.minecraft.core.BlockPos;

public interface SoyuzRenderSource {
    BlockPos renderCorePos();

    float renderPosition(int index, float partialTick);

    int renderSkin();

    BlockEntityLaunchpadSoyuz.SoyuzStatus renderStatus();

    default boolean renderSelected() {
        return true;
    }
}
