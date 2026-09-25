// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.items.weapon.sedna.AkimboGhost;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu {

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
