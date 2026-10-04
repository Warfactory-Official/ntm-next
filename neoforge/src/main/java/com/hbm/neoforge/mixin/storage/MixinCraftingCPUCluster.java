// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.blockentity.crafting.CraftingBlockEntity;
import com.hbm.inventory.InventoryChanges;
import com.hbm.neoforge.inventory.AE2CpuAnchor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "appeng.me.cluster.implementations.CraftingCPUCluster", remap = false)
public abstract class MixinCraftingCPUCluster implements AE2CpuAnchor {
    @Shadow
    private CraftingBlockEntity getCore() {
        throw new AssertionError();
    }

    @Override
    public BlockEntity craftingInventoryAnchor() {
        return getCore();
    }

    @Inject(method = "done", at = @At("RETURN"))
    private void hbm$formedAnchor(CallbackInfo ci) {
        InventoryChanges.storageLayoutChanged(getCore());
    }
}
