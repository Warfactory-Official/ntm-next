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

package com.hbm.handler.pollution;

import com.hbm.data.MobData;
import com.hbm.data.RadiationData;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PollutionSyncLayout;
import com.hbm.platform.PlayerLevelChange;
import com.hbm.platform.ServerEvents;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public final class PollutionHandler {
    public static final float SOOT_PER_SECOND = 1F / 25F;
    public static final float HEAVY_METAL_PER_SECOND = 1F / 50F;
    public static final float POISON_PER_SECOND = 1F / 50F;

    static final int EVOLUTION_PERIOD = 60;
    private static final int SURFACE_SAMPLES = 5;
    private static final int REGION_SPAN = 1 << PollutionSavedData.REGION_SHIFT;

    private static final Object2FloatOpenHashMap<UUID> SENT_SOOT = new Object2FloatOpenHashMap<>();
    private static final BlockPos.MutableBlockPos SURFACE_CELL = new BlockPos.MutableBlockPos();
    private static int sinceEvolution;

    static {
        SENT_SOOT.defaultReturnValue(Float.NaN);
    }

    private PollutionHandler() {}

    public static void install() {
        final Consumer<MinecraftServer> callback = PollutionHandler::tick;
        ServerEvents.TICK_POST.add(callback);
        final Consumer<ServerPlayer> callback2 = PollutionHandler::forget;
        ServerEvents.JOIN.add(callback2);
        final Consumer<ServerPlayer> callback3 = PollutionHandler::forget;
        ServerEvents.RESPAWN.add(callback3);
        final PlayerLevelChange callback4 =
                (player, _, _) -> forget(player);
        ServerEvents.LEVEL_CHANGE.add(callback4);
        final Consumer<ServerPlayer> callback5 = PollutionHandler::forget;
        ServerEvents.DISCONNECT.add(callback5);
        final Consumer<MinecraftServer> callback6 = _ -> SENT_SOOT.clear();
        ServerEvents.STOPPING.add(callback6);
    }

    public static void add(Level level, BlockPos pos, PollutionType kind, float amount) {
        if (!RadiationData.ENABLE_POLLUTION.get() || !(level instanceof ServerLevel server)) return;
        field(server)
                .add(
                        PollutionSavedData.region(pos.getX(), pos.getZ()),
                        kind.ordinal(),
                        amount,
                        multiplier());
    }

    public static void put(Level level, BlockPos pos, PollutionType kind, float value) {
        if (!RadiationData.ENABLE_POLLUTION.get() || !(level instanceof ServerLevel server)) return;
        field(server)
                .write(PollutionSavedData.region(pos.getX(), pos.getZ()), kind.ordinal(), value);
    }

    public static float read(Level level, BlockPos pos, PollutionType kind) {
        if (!RadiationData.ENABLE_POLLUTION.get() || !(level instanceof ServerLevel server))
            return 0F;
        var field = existing(server);
        if (field == null) return 0F;
        int slot = field.slot(PollutionSavedData.region(pos.getX(), pos.getZ()));
        return slot < 0 ? 0F : field.value(slot, kind.ordinal());
    }

    public static boolean sample(Level level, BlockPos pos, float[] into) {
        if (!RadiationData.ENABLE_POLLUTION.get() || !(level instanceof ServerLevel server))
            return false;
        var field = existing(server);
        if (field == null) return false;
        int slot = field.slot(PollutionSavedData.region(pos.getX(), pos.getZ()));
        if (slot < 0) return false;
        field.copy(slot, into);
        return true;
    }

    public static double multiplier() {
        double mult = RadiationData.POLLUTION_MULT.get();
        return MobData.RAMPANT_MODE.get() && mult == 1D ? 3D : mult;
    }

    public static double fogThreshold() {
        double threshold = RadiationData.SOOT_FOG_THRESHOLD.get();
        return MobData.RAMPANT_MODE.get() ? threshold * multiplier() : threshold;
    }

    static @Nullable PollutionSavedData existing(ServerLevel level) {
        var field = ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$pollution();
        if (field == null) {
            field = level.getDataStorage().get(PollutionSavedData.TYPE);
            if (field != null)
                ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$pollution(field);
        }
        return field;
    }

    private static PollutionSavedData field(ServerLevel level) {
        var field = existing(level);
        if (field == null) {
            field = level.getDataStorage().computeIfAbsent(PollutionSavedData.TYPE);
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$pollution(field);
        }
        return field;
    }

    private static void tick(MinecraftServer server) {
        if (RadiationData.ENABLE_POLLUTION.get()) {
            for (ServerLevel level : server.getAllLevels()) {
                var field = existing(level);
                if (field != null) damageSurface(level, field, level.getRandom());
            }
            if (++sinceEvolution >= EVOLUTION_PERIOD) {
                sinceEvolution = 0;
                for (ServerLevel level : server.getAllLevels()) {
                    var field = existing(level);
                    if (field != null && field.size() > 0) field.evolve();
                }
            }
        }
        var players = server.getPlayerList().getPlayers();
        for (int i = 0; i < players.size(); i++) syncSoot(players.get(i));
    }

    static void damageSurface(ServerLevel level, PollutionSavedData field, RandomSource random) {
        for (int i = 0; i < field.hotCount(); i++) {
            long region = field.hotRegion(i);
            int baseX = PollutionSavedData.regionX(region) << PollutionSavedData.REGION_SHIFT;
            int baseZ = PollutionSavedData.regionZ(region) << PollutionSavedData.REGION_SHIFT;
            for (int sample = 0; sample < SURFACE_SAMPLES; sample++) {
                int x = baseX + random.nextInt(REGION_SPAN);
                int z = baseZ + random.nextInt(REGION_SPAN);
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                int y =
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
                                - random.nextInt(3)
                                + 1;
                PollutionWorldHandler.destroyBlock(level, SURFACE_CELL.set(x, y, z));
            }
        }
    }

    private static void syncSoot(ServerPlayer player) {
        float soot = read(player.level(), player.blockPosition(), PollutionType.SOOT);
        var id = player.getUUID();
        if (SENT_SOOT.getFloat(id) == soot) return;
        SENT_SOOT.put(id, soot);
        PacketWire.sendTo(PollutionSyncLayout.encode(soot), player);
    }

    private static void forget(ServerPlayer player) {
        SENT_SOOT.removeFloat(player.getUUID());
    }
}
