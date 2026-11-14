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

import com.hbm.blockentity.Synced;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.qmaw.QMAWCatalog;
import com.hbm.qmaw.QuickManualAndWiki;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SyncPacketHandlers {
    private SyncPacketHandlers() {}

    public static void applyUnits(
            Player player,
            BlockPos pos,
            int blockEntityType,
            long revision,
            long base,
            ByteBuf body) {
        if (blockEntityType < 0 || revision <= 0 || base < 0 || base >= revision) {
            throw new DecoderException("Invalid machine unit revision");
        }
        BlockEntity be = ChunkUtil.blockEntityIfLoaded(player.level(), pos);
        boolean accepted =
                be instanceof Synced synced
                        && be instanceof SyncUnitSchema
                        && BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(be.getType())
                                == blockEntityType
                        && synced.applyUnitSync(revision, base, body);
        body.skipBytes(body.readableBytes());
        if (accepted) {
            ClientSyncRecovery.received(pos);
        } else {
            ClientSyncRecovery.request(player.level(), pos, blockEntityType);
        }
    }

    public static void applyBlob(
            Player player,
            BlockPos pos,
            int blockEntityType,
            long revision,
            long base,
            ByteBuf body) {
        if (blockEntityType < 0 || revision <= 0 || base < 0 || base >= revision) {
            throw new DecoderException("Invalid blob revision");
        }
        BlockEntity be = ChunkUtil.blockEntityIfLoaded(player.level(), pos);
        boolean accepted =
                be instanceof BlobSynced source
                        && BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(be.getType())
                                == blockEntityType
                        && source.syncBlob().apply(revision, base, body);
        body.skipBytes(body.readableBytes());
        if (accepted) {
            ClientSyncRecovery.received(pos, 2);
        } else {
            ClientSyncRecovery.request(player.level(), pos, blockEntityType, 2);
        }
    }

    public static ClientboundBlockEntityDataPacket recordUnit(
            BlockPos pos, int blockEntityType, long revision, long base, ByteBuf body) {
        return SyncReplay.units(pos, blockEntityType, revision, base, body);
    }

    public static ClientboundBlockEntityDataPacket recordBlob(
            BlockPos pos, int blockEntityType, long revision, long base, ByteBuf body) {
        return SyncReplay.blob(pos, blockEntityType, revision, base, body);
    }

    public static void publishBlueprintPools(List<String> pools) {
        RecipeSource.publishClientPoolNames(pools);
    }

    public static void publishRecipes(List<RecipeHolder<?>> recipes) {
        RecipeSource.publishClient(recipes, recipes);
    }

    public static void publishQmaw(Map<Identifier, QuickManualAndWiki> pages) {
        QMAWCatalog.publishRemote(new QMAWCatalog(pages));
    }
}
