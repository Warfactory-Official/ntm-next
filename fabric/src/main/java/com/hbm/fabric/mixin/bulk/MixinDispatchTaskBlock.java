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
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BulkEndLayout;
import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkSession;
import com.hbm.world.bulk.BulkSnapshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.DispatchTask;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DispatchTask.Block.class, remap = false)
public abstract class MixinDispatchTaskBlock {
    @Unique private final long hbm$session = BulkSession.nextClientId();
    @Unique private int hbm$sections;
    @Unique private boolean hbm$ended;
    @Unique private boolean hbm$refused;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hbm$permission(
            ResourceKey<Level> dimension, UUID uuid, BlockBuffer buffer, CallbackInfo ci) {
        if (AxiomClient.hasPermission(AxiomPermission.BUILD_NBT)) return;
        for (long section : buffer.keySet()) {
            var data = buffer.getBlockEntityChunkMap(section);
            if (data == null) continue;
            for (var value : data.values())
                if (value.decompress().contains(BulkSnapshot.KEY)) {
                    hbm$refused = true;
                    var player = Minecraft.getInstance().player;
                    if (player != null)
                        player.sendSystemMessage(
                                Component.translatable("commands.generic.permission"));
                    LogUtils.getLogger()
                            .warn(
                                    "NTM machine and graph snapshots require Axiom BUILD_NBT permission");
                    return;
                }
        }
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/moulberry/axiom/world_modification/BlockBuffer;getBlockEntityChunkMap(J)Lit/unimi/dsi/fastutil/shorts/Short2ObjectMap;"))
    private Short2ObjectMap<CompressedBlockEntity> hbm$header(
            BlockBuffer buffer,
            long section,
            Operation<Short2ObjectMap<CompressedBlockEntity>> original) {
        var data = original.call(buffer, section);
        var palette =
                buffer.getSection(
                        BlockPos.getX(section), BlockPos.getY(section), BlockPos.getZ(section));
        if (!palette.maybeHas(BulkAction::relevant)) return data;
        var result =
                data == null
                        ? new Short2ObjectOpenHashMap<CompressedBlockEntity>()
                        : new Short2ObjectOpenHashMap<>(data);
        var previous = result.get(AxiomBulkData.HEADER);
        CompoundTag tag = previous == null ? new CompoundTag() : previous.decompress();
        tag.putLong("hbm_session", hbm$session);
        tag.putInt("hbm_section", hbm$sections++);
        result.put(
                AxiomBulkData.HEADER,
                CompressedBlockEntity.compress(tag, new ByteArrayOutputStream()));
        return result;
    }

    @WrapMethod(method = "tick")
    private int hbm$end(int quota, Operation<Integer> original) {
        if (hbm$refused) return 0;
        int used = original.call(quota);
        if (!hbm$ended && hbm$sections != 0 && ((DispatchTask.Block) (Object) this).isDone()) {
            hbm$ended = true;
            PacketWire.sendToServer(BulkEndLayout.encode(hbm$session, hbm$sections));
        }
        return used;
    }

    @WrapMethod(method = "isDone")
    private boolean hbm$done(Operation<Boolean> original) {
        return hbm$refused || original.call();
    }
}
