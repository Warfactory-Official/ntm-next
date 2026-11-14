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

package com.hbm.items.machine.upgrade;

import org.jspecify.annotations.Nullable;

public enum UpgradeType {
    SPEED("upgrade.gui.speed"),
    EFFECT("upgrade.gui.effectiveness"),
    POWER("upgrade.gui.power"),
    FORTUNE(null),
    AFTERBURN("upgrade.gui.afterburner"),
    OVERDRIVE("upgrade.gui.overdrive"),
    STACK(null),
    EJECTOR(null),
    SPECIAL(null);

    private static final UpgradeType[] BY_ORDINAL = values();
    public static final int COUNT = BY_ORDINAL.length;

    public final @Nullable String panelKey;

    UpgradeType(@Nullable String panelKey) {
        this.panelKey = panelKey;
    }

    public static UpgradeType byOrdinal(int ordinal) {
        return BY_ORDINAL[ordinal];
    }
}
