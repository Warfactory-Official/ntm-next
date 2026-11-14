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

package com.hbm.packet.sync;

import com.hbm.packet.SyncWire;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SyncReplay {
    private SyncReplay() {}

    public static ClientboundBlockEntityDataPacket units(
            BlockPos pos, int blockEntityType, long revision, long base, ByteBuf body) {
        validate(blockEntityType, revision, base, "machine unit");
        return packet(pos, blockEntityType, base, SyncWire.FRAME_KEY, body);
    }

    public static ClientboundBlockEntityDataPacket blob(
            BlockPos pos, int blockEntityType, long revision, long base, ByteBuf body) {
        validate(blockEntityType, revision, base, "blob");
        return packet(pos, blockEntityType, base, SyncWire.BLOB_KEY, body);
    }

    private static void validate(int blockEntityType, long revision, long base, String kind) {
        if (blockEntityType < 0 || revision <= 0 || base < 0 || base >= revision) {
            throw new DecoderException("Invalid recorded " + kind + " revision");
        }
    }

    private static ClientboundBlockEntityDataPacket packet(
            BlockPos pos, int blockEntityType, long base, String key, ByteBuf body) {
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.byId(blockEntityType);
        if (type == null || BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(type) != blockEntityType) {
            throw new DecoderException("Unknown recorded block-entity type " + blockEntityType);
        }
        byte[] frame = new byte[Math.addExact(body.readableBytes(), 1)];
        frame[0] = (byte) (base == 0 ? 1 : 0);
        body.readBytes(frame, 1, frame.length - 1);
        CompoundTag tag = new CompoundTag();
        tag.putByteArray(key, frame);
        return new ClientboundBlockEntityDataPacket(pos, type, tag);
    }
}
