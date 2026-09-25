// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine.upgrade;

import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.tileentity.IUpgradeInfoProvider;
import java.util.Arrays;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class UpgradeManager implements SyncSource {

    private final IUpgradeInfoProvider provider;
    @SyncField private final int[] levels = new int[UpgradeType.VALUES.length];
    private final int[] nextLevels = new int[UpgradeType.VALUES.length];

    public UpgradeManager(IUpgradeInfoProvider provider) {
        this.provider = provider;
    }

    public void scan(Container container, int start, int end) {
        int[] caps = provider.getValidUpgrades();
        Arrays.fill(nextLevels, 0);

        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!(stack.getItem() instanceof ItemMachineUpgrade upgrade)) continue;
            int ord = upgrade.type.ordinal();
            int cap = caps[ord];
            if (cap <= 0) continue;
            nextLevels[ord] = Math.min(nextLevels[ord] + upgrade.tier, cap);
        }
        for (int i = 0; i < levels.length; i++) levels[i] = nextLevels[i];
    }

    public int getLevel(UpgradeType type) {
        return levels[type.ordinal()];
    }
}
