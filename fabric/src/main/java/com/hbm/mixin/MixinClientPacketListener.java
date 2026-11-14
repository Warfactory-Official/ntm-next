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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Shadow private ClientLevel level;

    @Inject(method = "handleSetEquipment", at = @At("RETURN"))
    private void hbm$armorEquipmentReceived(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {
        if (level.getEntity(packet.getEntity()) instanceof LivingEntity wearer)
            ((com.hbm.interfaces.injected.ArmorModWearer) (wearer)).hbm$reconcileArmorMods();
    }

    @Inject(method = "enableChunkLight", at = @At("RETURN"))
    private void hbm$sectionChunkLightQueued(LevelChunk chunk, int x, int z, CallbackInfo ci) {
        var geometry =
                ((com.hbm.interfaces.injected.ClientLevelExtension) (level))
                        .hbm$sectionGeometryIndex();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) geometry.lightChanged(x + dx, z + dz);
        }
    }

    @WrapOperation(
            method = "handleBlockEntityData",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Ljava/util/Optional;"))
    private Optional<BlockEntity> hbm$retainedEntity(
            ClientLevel level,
            BlockPos pos,
            BlockEntityType<?> type,
            Operation<Optional<BlockEntity>> original) {
        Optional<BlockEntity> current = original.call(level, pos, type);
        if (current.isPresent()) return current;
        BlockEntity retained =
                ((com.hbm.interfaces.injected.ClientLevelExtension) (level))
                        .hbm$retainedBlockEntity(pos);
        return retained != null && retained.getType() == type
                ? Optional.of(retained)
                : Optional.empty();
    }
}
