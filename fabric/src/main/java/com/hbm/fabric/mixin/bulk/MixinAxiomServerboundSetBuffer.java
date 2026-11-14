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

package com.hbm.fabric.mixin.bulk;

import com.hbm.fabric.integration.bulk.AxiomBulkData;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkPasteSessions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.packets.AxiomServerboundSetBuffer;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.world_modification.BlockBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AxiomServerboundSetBuffer.class, remap = false)
public abstract class MixinAxiomServerboundSetBuffer {
    @WrapMethod(method = "applyBlockBufferServer")
    private static void hbm$bulk(
            BlockBuffer buffer,
            ServerLevel level,
            ChunkedBooleanRegion placement,
            ServerPlayer player,
            Operation<Void> original) {
        long session = 0;
        if (player != null)
            for (long section : buffer.keySet()) {
                var data = buffer.getBlockEntityChunkMap(section);
                if (data == null) continue;
                var header = data.get(AxiomBulkData.HEADER);
                if (header == null) continue;
                var tag = header.decompress();
                if (!tag.contains("hbm_session")) continue;
                long id = tag.getLongOr("hbm_session", 0);
                if (session != 0 && session != id)
                    throw new IllegalArgumentException("Mixed Axiom bulk actions");
                session = id;
                BulkPasteSessions.received(player, id, tag.getIntOr("hbm_section", 0));
            }
        if (session == 0) {
            try (var bulk = BulkAction.begin()) {
                original.call(buffer, level, placement, player);
            }
        } else {
            try (var bulk = BulkAction.begin(BulkPasteSessions.session(player, session))) {
                original.call(buffer, level, placement, player);
            }
            BulkPasteSessions.complete(player, session);
        }
    }

    @WrapOperation(
            method = "applyBlockBufferServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState hbm$record(
            LevelChunkSection section,
            int x,
            int y,
            int z,
            BlockState state,
            boolean checked,
            Operation<BlockState> original,
            @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true, name = "buffer") BlockBuffer buffer,
            @Local(name = "bx") int bx,
            @Local(name = "by") int by,
            @Local(name = "bz") int bz,
            @Local(name = "canEditNbt") boolean canEditNbt) {
        BlockPos pos = new BlockPos(bx, by, bz);
        BulkAction.record(level, pos, section.getBlockState(x, y, z), state);
        BlockState previous = original.call(section, x, y, z, state, checked);
        var data = buffer.getBlockEntity(bx, by, bz);
        if (canEditNbt && data != null && BulkAction.relevant(state))
            BulkAction.imported(level, pos, data.decompress());
        return previous;
    }
}
