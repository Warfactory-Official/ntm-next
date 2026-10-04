// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.inventory.InventoryAlias;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler", remap = false)
public abstract class MixinUpgradeHandler implements InventoryAlias {
    @Shadow @Final private ContainerContents.UpgradeData upgradeData;

    @Override
    public Object inventoryIdentity() {
        return upgradeData;
    }
}
