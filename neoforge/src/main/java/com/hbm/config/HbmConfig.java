// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public interface HbmConfig {
    ContentConfig content();

    RuntimeConfig runtime();

    ConfigStore clientStore();

    <T> void set(ConfigEntry<T> entry, T value);

    void reload();

    default boolean pollForExternalEdit() {
        return false;
    }
}
