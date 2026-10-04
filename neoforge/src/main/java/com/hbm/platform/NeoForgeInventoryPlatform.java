// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import appeng.api.ids.AEComponents;
import com.hbm.integration.ae2.AE2BlockItemLayouts;
import com.hbm.integration.ae2.AE2ItemContents;
import com.hbm.inventory.ItemContents;
import com.hbm.inventory.SlotSource;
import com.hbm.neoforge.inventory.AE2MenuInventories;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.fml.ModList;

public final class NeoForgeInventoryPlatform {
    private static final boolean AE = ModList.get().isLoaded("ae2");

    public static void registerContents() {
        if (AE) {
            AE2ItemContents.register();
            AE2BlockItemLayouts.register();
            ItemContents.independentComponent(AEComponents.STORED_ENERGY);
        }
    }

    public static SlotSource menuInventory(AbstractContainerMenu menu) {
        return AE ? AE2MenuInventories.inventory(menu) : null;
    }
}
