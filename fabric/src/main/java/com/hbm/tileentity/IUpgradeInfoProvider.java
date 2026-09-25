// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.items.machine.upgrade.UpgradeType;

public interface IUpgradeInfoProvider {

    static int[] upgradeCaps(Object... pairs) {
        int[] caps = new int[UpgradeType.VALUES.length];
        for (int i = 0; i < pairs.length; i += 2) {
            caps[((UpgradeType) pairs[i]).ordinal()] = (Integer) pairs[i + 1];
        }
        return caps;
    }

    int[] getValidUpgrades();
}
