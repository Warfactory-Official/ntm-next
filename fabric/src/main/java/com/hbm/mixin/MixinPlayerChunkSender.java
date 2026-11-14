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

import com.hbm.entity.item.PendingFallers;
import com.hbm.packet.ChunkTrackerIndex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class MixinPlayerChunkSender {
    @WrapOperation(
            method = "sendNextChunks",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/network/PlayerChunkSender;sendChunk"
                                            + "(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"
                                            + "Lnet/minecraft/server/level/ServerLevel;"
                                            + "Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    private static void hbm$countChunkSent(
            ServerGamePacketListenerImpl connection,
            ServerLevel level,
            LevelChunk chunk,
            Operation<Void> original) {
        ChunkTrackerIndex.beginInitial(connection.player, chunk.getPos().pack());
        original.call(connection, level, chunk);
        ChunkTrackerIndex.endInitial();
        ChunkTrackerIndex.onChunkSent(level, connection.player, chunk.getPos().pack());
        PendingFallers.promote(level, chunk);
    }

    @Inject(method = "dropChunk", at = @At("HEAD"))
    private void hbm$countChunkDropped(ServerPlayer player, ChunkPos pos, CallbackInfo ci) {
        ChunkTrackerIndex.onChunkDropped(player.level(), player, pos.pack());
    }
}
