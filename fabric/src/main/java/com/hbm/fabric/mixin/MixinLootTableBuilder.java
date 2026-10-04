// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.world.loot.RandomSlotLoot;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootTable.Builder.class)
public abstract class MixinLootTableBuilder implements RandomSlotLoot {
    @Unique private boolean hbm$randomSlots;

    @Override
    public boolean hbm$randomSlots() {
        return hbm$randomSlots;
    }

    @Override
    public void hbm$randomSlots(boolean enabled) {
        hbm$randomSlots = enabled;
    }

    @Inject(method = "build", at = @At("RETURN"))
    private void hbm$preserveContainerFilling(CallbackInfoReturnable<LootTable> cir) {
        ((RandomSlotLoot) cir.getReturnValue()).hbm$randomSlots(hbm$randomSlots);
    }
}
