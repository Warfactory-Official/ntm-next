// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.FuseContainerOwner;
import com.hbm.inventory.StoredInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleContainer.class)
public abstract class MixinSimpleContainer implements FuseContainerOwner {
    @Unique private @Nullable Object hbm$fuseOwner;
    @Shadow @Final @Mutable public NonNullList<ItemStack> items;

    @Override
    public @Nullable Object hbm$fuseOwner() {
        return hbm$fuseOwner;
    }

    @Override
    public void hbm$fuseOwner(@Nullable Object owner) {
        hbm$fuseOwner = owner;
        if (owner != null && !(items instanceof StoredInventory))
            items = StoredInventory.track((SimpleContainer) (Object) this, items);
    }

    @Override
    public void hbm$trackSlots(Object source) {
        if (!(items instanceof StoredInventory tracked) || !tracked.owns(source))
            items = StoredInventory.track(source, items);
    }

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void hbm$contentsChanged(CallbackInfo ci) {
        assert hbm$fuseOwner == null || items instanceof StoredInventory;
    }
}
