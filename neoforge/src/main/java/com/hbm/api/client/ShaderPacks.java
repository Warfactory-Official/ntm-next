// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.client;

import net.irisshaders.iris.api.v0.IrisApi;
import net.neoforged.fml.ModList;

public final class ShaderPacks {
    private static final boolean IRIS = ModList.get().isLoaded("iris");

    private ShaderPacks() {}

    public static boolean inUse() {
        return IRIS && Iris.inUse();
    }

    private static final class Iris {
        private static boolean inUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
