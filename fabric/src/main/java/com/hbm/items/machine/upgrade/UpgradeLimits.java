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

public final class UpgradeLimits {

    public static final UpgradeLimits NONE = new UpgradeLimits(new int[UpgradeType.COUNT]);

    private final int[] max;

    private UpgradeLimits(int[] max) {
        this.max = max;
    }

    public static UpgradeLimits of(UpgradeType type, int max) {
        return NONE.and(type, max);
    }

    public UpgradeLimits and(UpgradeType type, int max) {
        int[] next = this.max.clone();
        next[type.ordinal()] = max;
        return new UpgradeLimits(next);
    }

    public int max(UpgradeType type) {
        return max[type.ordinal()];
    }
}
