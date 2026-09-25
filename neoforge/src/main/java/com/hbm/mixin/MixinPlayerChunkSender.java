// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

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
        try {
            original.call(connection, level, chunk);
        } finally {
            ChunkTrackerIndex.endInitial();
        }
        ChunkTrackerIndex.onChunkSent(level, connection.player, chunk.getPos().pack());
    }

    @Inject(method = "dropChunk", at = @At("HEAD"))
    private void hbm$countChunkDropped(ServerPlayer player, ChunkPos pos, CallbackInfo ci) {
        ChunkTrackerIndex.onChunkDropped(player.level(), player, pos.pack());
    }
}
