// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.debug.ItemGenBlockEntity;
import com.hbm.neoforge.inventory.LazyItemVariants;
import java.util.Queue;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemGenBlockEntity.class, remap = false, priority = 500)
public abstract class MixinItemGenBlockEntity {
    @Shadow @Final @Mutable private static Queue<ItemStack> SHARED_POSSIBLE_ITEMS;
    @Shadow @Final @Mutable private Queue<ItemStack> possibleItems;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void hbm$lazyGlobalVariants(CallbackInfo ci) {
        SHARED_POSSIBLE_ITEMS = new LazyItemVariants();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$lazyFilteredVariants(CallbackInfo ci) {
        possibleItems = new LazyItemVariants();
    }

    @Inject(method = "addPossibleItem", at = @At("HEAD"), cancellable = true)
    private static void hbm$registerVariants(Item item, Queue<ItemStack> queue, CallbackInfo ci) {
        ((LazyItemVariants) queue).register(item);
        ci.cancel();
    }
}
