// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.client;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

public final class ShaderPacks {
    private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");

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
