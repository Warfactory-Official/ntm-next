// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public interface ConfigStore {

    <T> T get(ConfigEntry<T> entry);

    record Content(ConfigStore store) implements ContentConfig {}

    record Runtime(ConfigStore store) implements RuntimeConfig {}
}
