// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal;

import com.hbm.handler.radiation.RadiationBackend;
import com.hbm.util.noise.SimplexBatch;
import java.util.ServiceLoader;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public interface Accelerator {

    @Nullable Accelerator INSTALLED =
            ServiceLoader.load(Accelerator.class, Accelerator.class.getClassLoader())
                    .findFirst()
                    .orElse(null);

    @Nullable SimplexBatch simplex();

    @Nullable RadiationBackend radiation(ServerLevel level);
}
