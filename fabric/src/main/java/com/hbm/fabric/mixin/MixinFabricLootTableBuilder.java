// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.world.loot.RandomSlotLoot;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FabricLootTableBuilder.class, remap = false)
public interface MixinFabricLootTableBuilder {
    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void hbm$copyContainerFilling(
            LootTable table, CallbackInfoReturnable<LootTable.Builder> cir) {
        ((RandomSlotLoot) cir.getReturnValue())
                .hbm$randomSlots(((RandomSlotLoot) table).hbm$randomSlots());
    }
}
