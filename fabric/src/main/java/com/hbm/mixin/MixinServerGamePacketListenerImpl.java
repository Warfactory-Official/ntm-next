// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {

    @Shadow public ServerPlayer player;

    @ModifyExpressionValue(
            method = "handleMovePlayer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;noBlocksAround(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean hbm$heldAloft(boolean noBlocksAround) {
        return noBlocksAround
                && !ArmorSuitEffects.heldAloft(player)
                && !ItemGunChargeThrower.anchored(player)
                && !player.onClimbable();
    }
}
