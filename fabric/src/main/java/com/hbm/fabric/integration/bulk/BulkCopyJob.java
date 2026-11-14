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

package com.hbm.fabric.integration.bulk;

import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BulkCopyRequestLayout;
import com.hbm.world.bulk.BulkCopyRequests;
import com.hbm.world.bulk.BulkSnapshot;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

public final class BulkCopyJob implements AutoCloseable {
    private final Long2ObjectOpenHashMap<Pending> chunks = new Long2ObjectOpenHashMap<>();

    public boolean ready(ChunkPos chunk) {
        Pending pending = chunks.get(chunk.pack());
        if (pending == null) {
            pending = new Pending();
            Pending response = pending;
            pending.id = BulkCopyRequests.register(response::accept);
            chunks.put(chunk.pack(), pending);
            PacketWire.sendToServer(BulkCopyRequestLayout.encode(pending.id, chunk.pack()));
        }
        if (pending.refused) {
            throw new IllegalStateException("NTM schematic copy refused by the server");
        }
        return pending.finished;
    }

    public boolean capture(ChunkPos chunk, BooleanSupplier body) {
        Pending pending = chunks.remove(chunk.pack());
        return BulkSnapshot.withClientSnapshot(pending.tags, body);
    }

    @Override
    public void close() {
        for (Pending pending : chunks.values()) {
            BulkCopyRequests.cancel(pending.id);
        }
        chunks.clear();
    }

    private static final class Pending {
        final Long2ObjectOpenHashMap<CompoundTag> tags = new Long2ObjectOpenHashMap<>();
        long id;
        boolean finished;
        boolean refused;

        private void accept(boolean success, CompoundTag data, boolean finished) {
            if (!success) {
                refused = true;
                return;
            }
            this.finished |= finished;
            for (String key : data.keySet()) {
                tags.put(Long.parseLong(key), data.getCompoundOrEmpty(key));
            }
        }
    }
}
