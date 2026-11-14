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

package com.hbm.mixin.compat.bulk;

import com.hbm.world.bulk.BulkAction;
import com.hbm.world.bulk.BulkHistory;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import nl.requios.effortlessbuilding.utilities.UndoManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = UndoManager.class, remap = false)
public abstract class MixinUndoManager {
    @Shadow @Final private static Map<UUID, Deque<UndoManager.UndoEntry>> undoStacks;
    @Shadow @Final private static Map<UUID, Deque<UndoManager.UndoEntry>> redoStacks;

    @WrapMethod(method = "recordOperation")
    private static void hbm$record(
            ServerPlayer player,
            ResourceKey<Level> dimension,
            Map<BlockPos, UndoManager.BlockChange> changes,
            Operation<Void> original) {
        BulkHistory history = BulkAction.history(dimension);
        if (history == null) {
            original.call(player, dimension, changes);
            return;
        }
        Map<BlockPos, UndoManager.BlockChange> expanded = new LinkedHashMap<>(changes);
        for (var entry : history.before().long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            var after = history.after().get(entry.getLongKey());
            if (entry.getValue().state() == after.state()
                    && Objects.equals(entry.getValue().tag(), after.tag())) expanded.remove(pos);
            else
                expanded.put(
                        pos, new UndoManager.BlockChange(entry.getValue().state(), after.state()));
        }
        if (!player.isCreative()) history.forgetReturnedItems(player.level(), false, history);
        original.call(player, dimension, expanded);
        if (!expanded.isEmpty())
            ((com.hbm.world.bulk.BulkHistoryCarrier)
                            (java.lang.Object) (undoStacks.get(player.getUUID()).peek()))
                    .hbm$bulkHistory(history);
    }

    @WrapMethod(method = "undo")
    private static int hbm$undo(ServerPlayer player, Operation<Integer> original) {
        return hbm$replay(player, undoStacks.get(player.getUUID()), true, original);
    }

    @WrapMethod(method = "redo")
    private static int hbm$redo(ServerPlayer player, Operation<Integer> original) {
        return hbm$replay(player, redoStacks.get(player.getUUID()), false, original);
    }

    private static int hbm$replay(
            ServerPlayer player,
            Deque<UndoManager.UndoEntry> stack,
            boolean undo,
            Operation<Integer> original) {
        UndoManager.UndoEntry entry = stack == null ? null : stack.peek();
        BulkHistory history =
                entry == null
                        ? null
                        : ((com.hbm.world.bulk.BulkHistoryCarrier) (java.lang.Object) (entry))
                                .hbm$bulkHistory();
        if (history == null) return original.call(player);
        var level = player.level().getServer().getLevel(entry.dimension());
        if (level == null) return original.call(player);
        try (var bulk = BulkAction.begin(player.isCreative())) {
            int result = original.call(player);
            if (!player.isCreative()) {
                BulkHistory current = BulkAction.history(entry.dimension());
                if (current != null) history.forgetReturnedItems(level, undo, current);
            }
            history.restore(level, undo, player.isCreative());
            return result;
        }
    }
}
