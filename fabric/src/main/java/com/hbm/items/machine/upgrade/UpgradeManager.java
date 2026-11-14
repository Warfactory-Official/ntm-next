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

import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import java.util.Arrays;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class UpgradeManager implements SyncSource {

    private final UpgradeInfoProvider provider;
    private final int[] levels = new int[UpgradeType.COUNT];
    private final int[] nextLevels = new int[UpgradeType.COUNT];

    public UpgradeManager(UpgradeInfoProvider provider) {
        this.provider = provider;
    }

    public void scan(Container container, int start, int end) {
        UpgradeLimits caps = provider.upgradeCaps();
        Arrays.fill(nextLevels, 0);

        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!(stack.getItem() instanceof MachineUpgradeItem upgrade)) continue;
            int ord = upgrade.type.ordinal();
            int cap = caps.max(upgrade.type);
            if (cap <= 0) continue;
            nextLevels[ord] = Math.min(nextLevels[ord] + upgrade.tier, cap);
        }
        for (int i = 0; i < levels.length; i++)
            hbm$syncArraySet$UpgradeManager$levels(levels, i, nextLevels[i]);
    }

    public int getLevel(UpgradeType type) {
        return levels[type.ordinal()];
    }

    public final int hbm$syncArraySet$UpgradeManager$levels(int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.levels) {
                syncArrayChanged(array, index, 3, 0L);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(UpgradeManager.class, "levels", 3, 0L);
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
