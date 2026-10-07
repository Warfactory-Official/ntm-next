// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import java.util.List;

public interface ConfigSchemaExtension {

    List<ConfigEntry<?>> entries();

    default void loadClient(ConfigStore client) {}
}
