// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import com.hbm.config.ConfigEntry.Domain;
import java.util.Locale;

public final class ConfigFiles {

    public static final String FABRIC_FILE = "hbm.jsonc";

    private ConfigFiles() {}

    public static String neoForgeFile(Domain domain) {
        return "hbm-" + domain.name().toLowerCase(Locale.ROOT) + ".toml";
    }

    public static String fabricSection(Domain domain) {
        return domain.name().toLowerCase(Locale.ROOT);
    }
}
