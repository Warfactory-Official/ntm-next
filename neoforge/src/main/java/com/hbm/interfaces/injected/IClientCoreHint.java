// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public interface IClientCoreHint {
    @Nullable BlockPos hbm$coreHint();

    void hbm$coreHint(@Nullable BlockPos pos);
}
