// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface IChunkExtension {

    String RADIATION_NBT_KEY = "hbm_rad";

    String CORE_INDEX_NBT_KEY = "hbm_core_index";

    String PENDING_FALLERS_NBT_KEY = "hbm_pending_fallers";

    byte @Nullable [] hbm$getRadiation();

    void hbm$setRadiation(byte @Nullable [] bytes);

    long @Nullable [] hbm$coreIndex();

    int hbm$coreIndexSize();

    void hbm$setCoreIndex(long @Nullable [] entries, int size);

    long @Nullable [] hbm$pendingFallerPositions();

    BlockState @Nullable [] hbm$pendingFallerStates();

    int hbm$pendingFallerSize();

    void hbm$setPendingFallers(
            long @Nullable [] positions, BlockState @Nullable [] states, int size);
}
