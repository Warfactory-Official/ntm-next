// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import org.jspecify.annotations.Nullable;

public interface IChunkExtension {

    String RADIATION_NBT_KEY = "hbm_rad";

    String CORE_INDEX_NBT_KEY = "hbm_core_index";

    byte @Nullable [] hbm$getRadiation();

    void hbm$setRadiation(byte @Nullable [] bytes);

    long @Nullable [] hbm$coreIndex();

    int hbm$coreIndexSize();

    void hbm$setCoreIndex(long @Nullable [] entries, int size);
}
