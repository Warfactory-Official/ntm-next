// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import com.hbm.platform.Services;
import java.util.function.Predicate;

public enum ForeignModChange {
    MEKANISM("mekanism", "mekanism_changes", ContentConfig::enableMekanismChanges),

    TECHREBORN("techreborn", "techreborn_changes", ContentConfig::enableTechRebornChanges);

    public final String modId;
    public final String pack;
    private final Predicate<ContentConfig> entry;

    ForeignModChange(String modId, String pack, Predicate<ContentConfig> entry) {
        this.modId = modId;
        this.pack = pack;
        this.entry = entry;
    }

    public boolean enabled() {
        ContentConfig config = Services.CONFIG.content();
        return config.enableForeignModChanges() && entry.test(config);
    }

    public String titleKey() {
        return "pack.hbm." + pack;
    }
}
