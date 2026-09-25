// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.ctm;

import com.hbm.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum CtmEngine {
    CONTINUITY("continuity", "Continuity", "ctm_continuity"),
    FUSION("fusion", "Fusion", "ctm_fusion"),
    CTM_LIB("ctm", "CTM Lib", "ctm_lib"),
    NONE("", "", "");

    private final String mod;
    private final String title;
    private final String pack;

    CtmEngine(String mod, String title, String pack) {
        this.mod = mod;
        this.title = title;
        this.pack = pack;
    }

    public static CtmEngine selected() {
        return Selection.ENGINE;
    }

    public Identifier packId() {
        return Identifier.fromNamespaceAndPath("hbm", pack);
    }

    public String directory() {
        return "resourcepacks/" + packId().getPath();
    }

    public Component title() {
        return Component.translatable("pack.hbm.ctm", title);
    }

    private static final class Selection {
        private static final CtmEngine ENGINE = select();

        private static CtmEngine select() {
            for (CtmEngine engine : values()) {
                if (engine != NONE && Services.PLATFORM.isModLoaded(engine.mod)) return engine;
            }
            return NONE;
        }
    }
}
