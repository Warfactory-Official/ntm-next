// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.neoforge.inventory.SophisticatedContentsStorage;
import com.hbm.neoforge.inventory.SophisticatedItemInteriors;
import java.util.Map;
import java.util.UUID;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemContentsStorage.class, remap = false)
public abstract class MixinItemContentsStorage implements SophisticatedContentsStorage {
    @Shadow @Final private Map<UUID, ContainerContents> storageContents;

    @Override
    public @Nullable ContainerContents storedContents(UUID id) {
        return storageContents.get(id);
    }

    @Inject(method = "getOrCreateContents", at = @At("HEAD"), cancellable = true)
    private void hbm$existingContents(UUID id, CallbackInfoReturnable<ContainerContents> ci) {
        ContainerContents current = storageContents.get(id);
        if (current != null) ci.setReturnValue(current);
    }

    @Inject(method = "getOrCreateContents", at = @At("RETURN"))
    private void hbm$createdContents(UUID id, CallbackInfoReturnable<ContainerContents> ci) {
        SophisticatedItemInteriors.storageRecordChanged(id);
    }

    @Inject(method = "setContents", at = @At("RETURN"))
    private void hbm$recordReplaced(UUID id, ContainerContents contents, CallbackInfo ci) {
        SophisticatedItemInteriors.storageRecordChanged(id);
    }

    @Inject(method = "removeContents", at = @At("RETURN"))
    private void hbm$recordRemoved(UUID id, CallbackInfo ci) {
        SophisticatedItemInteriors.storageRecordChanged(id);
    }
}
