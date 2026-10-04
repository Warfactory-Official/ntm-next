// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.shadows.ShadowRenderingState;

public final class RibbonPass {
    private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");

    private RibbonPass() {}

    public static boolean isShadow() {
        return IRIS && IrisShadow.active();
    }

    public static boolean hasShaderPack() {
        return IRIS && IrisShadow.hasShaderPack();
    }

    private static final class IrisShadow {
        private static boolean active() {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
        }

        private static boolean hasShaderPack() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
