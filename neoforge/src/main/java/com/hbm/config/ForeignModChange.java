// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import java.util.function.Predicate;

public enum ForeignModChange {
    MEKANISM("mekanism", "mekanism_changes", ContentConfig::enableMekanismChanges, true),
    TECHREBORN("techreborn", "techreborn_changes", ContentConfig::enableTechRebornChanges, false);

    public final String modId;
    public final String pack;
    public final boolean neoForge;
    private final Predicate<ContentConfig> entry;

    ForeignModChange(String modId, String pack, Predicate<ContentConfig> entry, boolean neoForge) {
        this.modId = modId;
        this.pack = pack;
        this.entry = entry;
        this.neoForge = neoForge;
    }

    public boolean enabled() {
        ContentConfig config = NeoForgeHbmConfig.CONTENT;
        return config.enableForeignModChanges() && entry.test(config);
    }

    public String titleKey() {
        return "pack.hbm." + pack;
    }
}
