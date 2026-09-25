// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.SyncRequestPayload;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClientSyncRecovery {
    private static final Long2ObjectOpenHashMap<Pending> PENDING = new Long2ObjectOpenHashMap<>();
    private static Level level;
    private static int tick;

    private ClientSyncRecovery() {}

    public static void request(Level current, BlockPos pos, int type) {
        request(current, pos, type, 1);
    }

    public static void request(Level current, BlockPos pos, int type, int parts) {
        if (level != current) {
            PENDING.clear();
            level = current;
        }
        Pending pending = PENDING.get(pos.asLong());
        if (pending == null || pending.type != type)
            PENDING.put(pos.asLong(), new Pending(pos, type, tick, parts));
        else pending.parts |= parts;
    }

    public static void received(BlockPos pos) {
        received(pos, 1);
    }

    public static void received(BlockPos pos, int parts) {
        Pending pending = PENDING.get(pos.asLong());
        if (pending != null && (pending.parts &= ~parts) == 0) PENDING.remove(pos.asLong());
    }

    public static void tick() {
        Level current = Minecraft.getInstance().level;
        if (level != current) {
            PENDING.clear();
            level = current;
        }
        tick++;
        if (PENDING.isEmpty()) return;
        var entries = PENDING.values().iterator();
        while (entries.hasNext()) {
            Pending pending = entries.next();
            if (tick - pending.created > 200) {
                entries.remove();
                continue;
            }
            if (tick < pending.next) continue;
            BlockEntity be = ChunkUtil.blockEntityIfLoaded(current, pending.pos);
            if (be == null
                    || BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(be.getType()) != pending.type)
                continue;
            PacketWire.sendToServer(new SyncRequestPayload(pending.pos));
            pending.next = tick + 20;
        }
    }

    private static final class Pending {
        final BlockPos pos;
        final int type, created;
        int next;
        int parts;

        Pending(BlockPos pos, int type, int tick, int parts) {
            this.pos = pos;
            this.type = type;
            this.created = tick;
            this.parts = parts;
        }
    }
}
