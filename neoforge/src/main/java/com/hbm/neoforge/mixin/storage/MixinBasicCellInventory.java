// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.cells.BasicCellInventory;
import com.hbm.integration.ae2.AE2ItemContents;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class MixinBasicCellInventory {
    @Shadow @Final private ItemStack i;

    @ModifyReturnValue(
            method = {"insert", "extract"},
            at = @At("RETURN"))
    private long hbm$publishArmedCellChange(
            long moved, AEKey key, long amount, Actionable mode, IActionSource source) {
        AE2ItemContents.persistArmedChange((BasicCellInventory) (Object) this, i, key, moved, mode);
        return moved;
    }
}
