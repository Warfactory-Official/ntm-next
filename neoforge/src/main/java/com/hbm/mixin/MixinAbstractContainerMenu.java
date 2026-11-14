/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin;

import com.hbm.interfaces.injected.FuseContainerOwner;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu implements FuseContainerOwner {
    @Unique private @Nullable Object hbm$fuseOwner;

    @Override
    public @Nullable Object hbm$fuseOwner() {
        return hbm$fuseOwner;
    }

    @Override
    public void hbm$fuseOwner(@Nullable Object owner) {
        hbm$fuseOwner = owner;
    }

    @WrapOperation(
            method = "setCarried",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/world/inventory/AbstractContainerMenu;carried:Lnet/minecraft/world/item/ItemStack;",
                            opcode = Opcodes.PUTFIELD))
    private void hbm$cursorChanged(
            AbstractContainerMenu self, ItemStack stack, Operation<Void> original) {
        if (self.getCarried() != stack) InventorySlotBinding.unbind(self.getCarried(), self);
        original.call(self, stack);
        if (hbm$fuseOwner != null) InventoryChanges.publishedStack(self, -1, stack);
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void hbm$refuseAkimboGhostSwap(
            int slotIndex,
            int buttonNum,
            ContainerInput containerInput,
            Player player,
            CallbackInfo ci) {
        if (containerInput == ContainerInput.SWAP
                && buttonNum == Inventory.SLOT_OFFHAND
                && AkimboGhost.isGhost(player.getInventory().getItem(Inventory.SLOT_OFFHAND))) {
            ci.cancel();
        }
    }
}
