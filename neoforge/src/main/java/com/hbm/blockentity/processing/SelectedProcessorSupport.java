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

package com.hbm.blockentity.processing;

import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.machine.ModuleMachineBase;

public final class SelectedProcessorSupport {
    public static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 3);

    private SelectedProcessorSupport() {}

    public static double speed(UpgradeManager upgrades) {
        return 1D
                + upgrades.getLevel(UpgradeType.SPEED) / 3D
                + upgrades.getLevel(UpgradeType.OVERDRIVE);
    }

    public static double power(UpgradeManager upgrades) {
        return 1D
                - upgrades.getLevel(UpgradeType.POWER) * 0.25D
                + upgrades.getLevel(UpgradeType.SPEED)
                + upgrades.getLevel(UpgradeType.OVERDRIVE) * (10D / 3D);
    }

    public static boolean select(ModuleMachineBase module, String recipe, boolean restricted) {
        var previous = module.getRecipeName();
        boolean previousMode = module.restrictedMode;
        module.setRecipe(recipe, restricted);
        return previousMode != module.restrictedMode || !previous.equals(module.getRecipeName());
    }

    public static long capacity(ModuleMachineBase module, long previous, long stored, long floor) {
        var selected = module.getRecipe();
        return Math.max(
                Math.max(selected == null ? previous : selected.power * 100L, stored), floor);
    }
}
