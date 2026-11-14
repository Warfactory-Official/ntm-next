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
        ContentConfig config = HbmConfig.CONTENT;
        return config.enableForeignModChanges() && entry.test(config);
    }

    public String titleKey() {
        return "pack.hbm." + pack;
    }
}
