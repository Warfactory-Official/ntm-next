/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
