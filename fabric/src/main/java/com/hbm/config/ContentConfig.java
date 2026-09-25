// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public interface ContentConfig {

    ConfigStore store();

    default boolean enableForeignModChanges() {
        return store().get(ConfigSchema.ENABLE_FOREIGN_MOD_CHANGES);
    }

    default boolean enableMekanismChanges() {
        return store().get(ConfigSchema.ENABLE_MEKANISM_CHANGES);
    }

    default boolean enableTechRebornChanges() {
        return store().get(ConfigSchema.ENABLE_TECHREBORN_CHANGES);
    }
}
