// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.platform.Services;
import net.irisshaders.iris.shadows.ShadowRenderingState;

public final class RibbonPass {
    private static final boolean IRIS = Services.PLATFORM.isModLoaded("iris");

    private RibbonPass() {}

    public static boolean isShadow() {
        return IRIS && IrisShadow.active();
    }

    private static final class IrisShadow {
        private static boolean active() {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
        }
    }
}
