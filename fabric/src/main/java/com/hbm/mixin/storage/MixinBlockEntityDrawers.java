// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.TrackedStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers",
        remap = false)
public abstract class MixinBlockEntityDrawers {
    @Inject(method = "onAttributeChanged", at = @At("RETURN"))
    private void hbm$changedSlotOwnership(CallbackInfo ci) {
        if ((Object) this instanceof TrackedStorage)
            InventoryChanges.storageLayoutChanged((BlockEntity) (Object) this);
    }
}
