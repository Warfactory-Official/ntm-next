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

import com.hbm.interfaces.injected.FarViewer;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.weapon.sedna.impl.GunChargeThrowerItem;
import com.hbm.packet.BulkTransfers;
import com.hbm.world.bulk.BulkPasteSessions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl implements FarViewer {

    @Shadow public ServerPlayer player;
    @Unique private int hbm$farView;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void hbm$closeBulkTransfers(DisconnectionDetails details, CallbackInfo ci) {
        BulkPasteSessions.disconnected(player);
        BulkTransfers.disconnected(player);
    }

    @Override
    public int hbm$farView() {
        return hbm$farView;
    }

    @Override
    public void hbm$farView(int blocks) {
        hbm$farView = blocks;
    }

    @ModifyExpressionValue(
            method = "handlePlayerPositionChange(DDDFFZZ)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;noBlocksAround(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean hbm$heldAloft(boolean noBlocksAround) {
        return noBlocksAround
                && !ArmorSuitEffects.heldAloft(player)
                && !GunChargeThrowerItem.anchored(player)
                && !player.onClimbable();
    }
}
