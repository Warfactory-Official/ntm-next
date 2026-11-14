/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.handler;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.injected.ClientImpactStateAccess;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ImpactSyncLayout;
import com.hbm.platform.Platform;
import com.hbm.platform.PlayerLevelChange;
import com.hbm.platform.ServerEvents;
import com.hbm.registration.OverridePoint;
import com.hbm.saveddata.TomSaveData;
import com.hbm.util.ChunkUtil;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ImpactWorldHandler {
    public static final OverridePoint<Predicate<ServerLevel>> EXTINCTION_LEVELS =
            OverridePoint.of(
                    RegistryUtil.id("impact_extinction_levels"),
                    level -> level.dimension() == Level.OVERWORLD);

    private static final float SETTLE = 1F / 14400000F;
    private static final float COOL = 1F / 24000F;

    private static final List<LevelChunk> LOADED = new ArrayList<>();
    private static final Map<UUID, Snapshot> SENT = new HashMap<>();

    private ImpactWorldHandler() {}

    public static void init() {
        final Consumer<MinecraftServer> callback = ImpactWorldHandler::onServerTick;
        ServerEvents.TICK_PRE.add(callback);
        final Consumer<ServerPlayer> callback2 = player -> SENT.remove(player.getUUID());
        ServerEvents.JOIN.add(callback2);
        final Consumer<ServerPlayer> callback3 = player -> SENT.remove(player.getUUID());
        ServerEvents.RESPAWN.add(callback3);
        final PlayerLevelChange callback4 =
                (player, origin, destination) -> SENT.remove(player.getUUID());
        ServerEvents.LEVEL_CHANGE.add(callback4);
        final Consumer<ServerPlayer> callback5 = player -> SENT.remove(player.getUUID());
        ServerEvents.DISCONNECT.add(callback5);
        final Consumer<MinecraftServer> callback6 = server -> SENT.clear();
        ServerEvents.STOPPING.add(callback6);
    }

    private static void onServerTick(MinecraftServer server) {
        if (simulationTick(server))
            for (ServerLevel level : server.getAllLevels()) {
                TomSaveData data = TomSaveData.getExisting(level);
                if (data == null) continue;
                impactEffects(level, data);
                settle(data);
            }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TomSaveData data = TomSaveData.getExisting(player.level());
            Snapshot current =
                    data == null ? new Snapshot(0F, 0F) : new Snapshot(data.fire, data.dust);
            if (current.equals(SENT.put(player.getUUID(), current))) continue;
            PacketWire.sendTo(ImpactSyncLayout.encode(current.fire, current.dust), player);
        }
    }

    public static boolean simulationTick(MinecraftServer server) {
        var tickRate = server.tickRateManager();
        return !tickRate.isFrozen() || tickRate.isSteppingForward();
    }

    public static void settle(TomSaveData data) {
        if (data.dust > 0 && data.fire == 0) {
            data.dust = Math.max(0, data.dust - SETTLE);
            data.setDirty();
        }

        if (data.fire > 0) {
            data.fire = Math.max(0, data.fire - COOL);
            data.dust = Math.min(1, data.dust + COOL);
            data.setDirty();
        }
    }

    public static void livingTick(ServerLevel level, LivingEntity entity) {
        if (!EXTINCTION_LEVELS.get().test(level)) return;
        TomSaveData data = TomSaveData.published(level);
        if (data != null && data.fire > 0 && data.dust < 0.75F) burnIfExposed(level, entity);
    }

    public static void burnIfExposed(ServerLevel level, LivingEntity entity) {
        if (level.getBrightness(LightLayer.SKY, entity.blockPosition()) > 7) {
            entity.igniteForSeconds(5F);
            entity.hurtServer(level, level.damageSources().onFire(), 2F);
        }
    }

    private static void impactEffects(ServerLevel level, TomSaveData data) {
        if (level.dimension() != Level.OVERWORLD) return;
        if (data.dust <= 0 && data.fire <= 0) return;

        ChunkUtil.forEachLoadedChunk(level, LOADED::add);
        int loaded = LOADED.size();

        if (loaded > 0) {
            RandomSource rand = level.getRandom();
            for (int i = 0; i < 3; i++) {
                ChunkPos coord = LOADED.get(rand.nextInt(loaded)).getPos();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (rand.nextBoolean()) continue;

                        int blockX = coord.getMinBlockX() + x;
                        int blockZ = coord.getMinBlockZ() + z;
                        int height =
                                level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
                        int blockY = height - rand.nextInt(Math.max(1, height - level.getMinY()));
                        BlockPos pos = new BlockPos(blockX, blockY, blockZ);

                        if (data.dust > 0) die(level, pos, data.dust);
                        if (data.fire > 0) burn(level, pos);
                    }
                }
            }
        }
        LOADED.clear();
    }

    public static void die(ServerLevel level, BlockPos pos, float dust) {
        BlockPos above = pos.above();
        int light =
                Math.max(
                        level.getBrightness(LightLayer.BLOCK, above),
                        (int) (level.getMaxLocalRawBrightness(above) * (1 - dust)));

        if (light < 4) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (state.is(Blocks.GRASS_BLOCK)) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            } else if (block instanceof VegetationBlock
                    || block instanceof LeavesBlock
                    || block instanceof VineBlock) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    public static void burn(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        BlockPos above = pos.above();
        boolean exposed = level.getBrightness(LightLayer.SKY, above) >= 7;

        if (Platform.isFlammable(level, pos, state, Direction.UP)
                && level.getBlockState(above).isAir()
                && exposed) {
            if (block instanceof LeavesBlock || block instanceof VegetationBlock) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
        } else if ((state.is(Blocks.GRASS_BLOCK)
                        || state.is(Blocks.MYCELIUM)
                        || state.is(ModBlocks.WASTE_EARTH.get())
                        || state.is(ModBlocks.FROZEN_GRASS.get())
                        || state.is(ModBlocks.WASTE_MYCELIUM.get()))
                && !level.isRainingAt(above)
                && exposed) {
            level.setBlockAndUpdate(pos, ModBlocks.BURNING_EARTH.get().defaultBlockState());

        } else if (state.is(ModBlocks.FROZEN_DIRT.get()) && exposed) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }

    public static boolean deniesSpawn(ServerLevel level, Mob mob) {
        TomSaveData data = TomSaveData.getExisting(level);
        if (data == null || !data.impact) return false;
        boolean water = mob instanceof WaterAnimal || mob instanceof AgeableWaterCreature;
        if (EXTINCTION_LEVELS.get().test(level)
                && (mob.getBbHeight() >= 0.85F
                        || mob.getBbWidth() >= 0.85F && !water && !mob.isBaby())) {
            return true;
        }
        return water && level.getRandom().nextInt(5) != 0;
    }

    private record Snapshot(float fire, float dust) {}

    public static void receive(Player player, float fire, float dust) {
        ((ClientImpactStateAccess) player.level()).hbm$setImpactState(fire, dust);
    }

    public static float getFireForClient(Level level) {
        return level instanceof ClientImpactStateAccess state ? state.hbm$impactFire() : 0F;
    }

    public static float getDustForClient(Level level) {
        return level instanceof ClientImpactStateAccess state ? state.hbm$impactDust() : 0F;
    }
}
