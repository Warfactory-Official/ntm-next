/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.advancement.AwardRegions;
import com.hbm.api.environment.Environment;
import com.hbm.blockentity.prop.PedestalBlockEntity.CharmKind;
import com.hbm.blockentity.prop.PedestalBlockEntity.CharmLease;
import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.data.WorldData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.RADBeastEntity;
import com.hbm.entity.projectile.MeteorEntity;
import com.hbm.extprop.LivingHazards;
import com.hbm.extprop.PlayerState;
import com.hbm.items.ModItems;
import com.hbm.platform.Platform;
import com.hbm.platform.ServerEvents;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DropHeight;
import com.hbm.world.gen.WorldgenHeight;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BossSpawnHandler {
    private static final RandomSource meteorRand = RandomSource.create();

    public static int meteorShower = 0;

    private BossSpawnHandler() {}

    public static void init() {
        final Consumer<MinecraftServer> callback = BossSpawnHandler::onServerTick;
        ServerEvents.TICK_PRE.add(callback);
        final Consumer<ServerPlayer> callback2 =
                player -> PlayerState.getOrCreate(player).maskManTimer = 0;
        ServerEvents.RESPAWN.add(callback2);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) rollTheDice(level);
    }

    public static void rollTheDice(ServerLevel level) {
        RandomSource random = level.getRandom();
        long time = level.getGameTime();
        boolean surface = level.dimension() != Level.NETHER && level.dimension() != Level.END;
        List<ServerPlayer> players = level.getPlayers(EntitySelector.NO_SPECTATORS);

        if (MobData.ENABLE_MASKMAN.get()
                && time % 20 == 0
                && surface
                && level.getDifficulty() != Difficulty.PEACEFUL) {
            for (ServerPlayer player : players) maskManUpdate(level, player);
        }

        if (MobData.ENABLE_RAIDS.get()
                && time % MobData.RAID_DELAY.get() == 0
                && random.nextInt(MobData.RAID_CHANCE.get()) == 0
                && !players.isEmpty()
                && surface) {
            raid(level, players.get(random.nextInt(players.size())));
        }

        if (MobData.ENABLE_ELEMENTALS.get()
                && time % MobData.ELEMENTAL_DELAY.get() == 0
                && random.nextInt(MobData.ELEMENTAL_CHANCE.get()) == 0
                && !players.isEmpty()
                && surface) {
            elementals(level, players.get(random.nextInt(players.size())));
        }

        if (WorldData.ENABLE_METEOR_STRIKES.get()) meteorUpdate(level);

        if (time % 20 == 0 && random.nextInt(5) == 0 && !players.isEmpty() && surface) {
            ghost(level, players.get(random.nextInt(players.size())));
        }
    }

    public static void maskManUpdate(ServerLevel level, ServerPlayer player) {
        Item acidizer = ModBlocks.MACHINE_CRYSTALLIZER.get().asItem();
        ServerStatsCounter stats = player.getStats();
        boolean acidizerStat =
                stats.getValue(Stats.ITEM_CRAFTED.get(acidizer)) > 0
                        || stats.getValue(Stats.ITEM_USED.get(acidizer)) > 0;
        boolean hasRads = ContaminationUtil.getRads(player) >= MobData.MASKMAN_MIN_RAD.get();
        boolean underground = underground(level, player) || !MobData.MASKMAN_UNDERGROUND.get();
        PlayerState data = PlayerState.getOrCreate(player);

        if (!(acidizerStat && hasRads && underground)) {
            data.maskManTimer = 0;
            return;
        }

        data.maskManTimer++;

        if (data.maskManTimer == MobData.MASKMAN_DELAY.get() - 60) {
            player.sendSystemMessage(
                    Component.translatable("chat.bossSpawnHandler.theMaskManDraws")
                            .withStyle(ChatFormatting.RED));
        }

        if (data.maskManTimer >= MobData.MASKMAN_DELAY.get()) {
            data.maskManTimer = 0;

            RandomSource random = level.getRandom();
            double spawnX = player.getX() + random.nextGaussian() * 20;
            double spawnZ = player.getZ() + random.nextGaussian() * 20;
            if (trySpawn(
                    level,
                    spawnX,
                    0D,
                    spawnZ,
                    Mth.floor(spawnX),
                    Mth.floor(spawnZ),
                    ModEntities.MASK_MAN.get().create(level, EntitySpawnReason.EVENT))) {
                player.sendSystemMessage(
                        Component.translatable("chat.bossSpawnHandler.theMaskManIs")
                                .withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(
                        Component.translatable("chat.bossSpawnHandler.seemsLikeMaskMan")
                                .withStyle(ChatFormatting.BLUE));
            }
        }
    }

    public static void raid(ServerLevel level, ServerPlayer player) {
        if (PlayerState.getOrCreate(player).fbiMark >= level.getGameTime()) return;

        RandomSource random = level.getRandom();
        player.sendSystemMessage(
                Component.translatable("chat.bossSpawnHandler.fbiOpenUp")
                        .withStyle(ChatFormatting.RED));

        Vec3 vec =
                new Vec3(MobData.RAID_ATTACK_DISTANCE.get(), 0, 0)
                        .yRot((float) (Math.PI * 2) * random.nextFloat());

        for (int i = 0; i < MobData.RAID_AMOUNT.get(); i++) {
            double spawnX = player.getX() + vec.x + random.nextGaussian() * 5;
            double spawnZ = player.getZ() + vec.z + random.nextGaussian() * 5;
            trySpawn(
                    level,
                    spawnX,
                    0D,
                    spawnZ,
                    (int) spawnX,
                    (int) spawnZ,
                    ModEntities.FBI.get().create(level, EntitySpawnReason.EVENT));
        }

        for (int i = 0; i < MobData.RAID_DRONES.get(); i++) {
            double spawnX = player.getX() + vec.x + random.nextGaussian() * 5;
            double spawnZ = player.getZ() + vec.z + random.nextGaussian() * 5;
            trySpawn(
                    level,
                    spawnX,
                    10D,
                    spawnZ,
                    (int) spawnX,
                    (int) spawnZ,
                    ModEntities.FBI_DRONE.get().create(level, EntitySpawnReason.EVENT));
        }
    }

    public static void elementals(ServerLevel level, ServerPlayer player) {
        PlayerState data = PlayerState.getOrCreate(player);
        if (!data.radMark) return;

        RandomSource random = level.getRandom();
        player.sendSystemMessage(
                Component.translatable("chat.bossSpawnHandler.youHearAFaint")
                        .withStyle(ChatFormatting.YELLOW));
        data.radMark = false;

        Vec3 vec = new Vec3(MobData.ELEMENTAL_DISTANCE.get(), 0, 0);

        for (int i = 0; i < MobData.ELEMENTAL_AMOUNT.get(); i++) {
            vec = vec.yRot((float) (Math.PI * 2) * random.nextFloat());

            double spawnX = player.getX() + vec.x + random.nextGaussian();
            double spawnZ = player.getZ() + vec.z + random.nextGaussian();

            RADBeastEntity rad = ModEntities.RAD_BEAST.get().create(level, EntitySpawnReason.EVENT);
            if (rad == null) continue;
            if (i == 0) rad.makeLeader();

            trySpawn(level, spawnX, 0D, spawnZ, (int) spawnX, (int) spawnZ, rad);
        }
    }

    public static void ghost(ServerLevel level, ServerPlayer player) {
        if (LivingHazards.digamma(player) <= 0) return;

        RandomSource random = level.getRandom();
        Vec3 vec = new Vec3(75, 0, 0).yRot((float) (Math.PI * 2) * random.nextFloat());
        double spawnX = player.getX() + vec.x + random.nextGaussian();
        double spawnZ = player.getZ() + vec.z + random.nextGaussian();
        trySpawn(
                level,
                spawnX,
                0D,
                spawnZ,
                (int) spawnX,
                (int) spawnZ,
                ModEntities.GHOST.get().create(level, EntitySpawnReason.EVENT));
    }

    public static void markFBI(Player player) {
        if (!player.level().isClientSide()) {
            PlayerState.getOrCreate(player).fbiMark = player.level().getGameTime() + 20 * 60 * 20;
        }
    }

    public static void markRad(ServerLevel level, double x, double y, double z) {
        if (!MobData.ENABLE_ELEMENTALS.get()) return;
        AwardRegions.within(
                level, x, y, z, 100, player -> PlayerState.getOrCreate(player).radMark = true);
    }

    private static boolean underground(ServerLevel level, Player player) {
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        return chunk != null && WorldgenHeight.lightBlocking(chunk, x, z) > player.getY() + 3;
    }

    private static boolean trySpawn(
            ServerLevel level,
            double x,
            double yAbove,
            double z,
            int columnX,
            int columnZ,
            @Nullable Mob mob) {
        if (mob == null) return false;
        LevelChunk chunk = level.getChunkSource().getChunkNow(columnX >> 4, columnZ >> 4);
        if (chunk == null) return false;
        mob.snapTo(
                x,
                WorldgenHeight.lightBlocking(chunk, columnX, columnZ) + yAbove,
                z,
                level.getRandom().nextFloat() * 360.0F,
                0.0F);
        Platform.finalizeSpawn(
                mob,
                level,
                level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.EVENT,
                null);
        return level.addFreshEntity(mob);
    }

    private static void meteorUpdate(ServerLevel level) {

        if (meteorRand.nextInt(
                        meteorShower > 0
                                ? WorldData.METEOR_SHOWER_CHANCE.get()
                                : WorldData.METEOR_STRIKE_CHANCE.get())
                == 0) {

            List<ServerPlayer> players = level.getPlayers(EntitySelector.NO_SPECTATORS);

            if (!players.isEmpty()) {

                Player p = players.get(meteorRand.nextInt(players.size()));

                if (p != null && Environment.meteorStrikes(level)) {

                    boolean repell = false;
                    boolean strike = true;

                    for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
                        ItemStack armor = p.getItemBySlot(slot);
                        if (!armor.isEmpty() && ArmorModHandler.hasMods(armor)) {
                            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                                if (mod.isEmpty()) continue;
                                if (mod.is(ModItems.PROTECTION_CHARM.get())) repell = true;
                                if (mod.is(ModItems.METEOR_CHARM.get())) strike = false;
                            }
                        }
                    }

                    if (!repell || strike) {
                        int x = Mth.floor(p.getX());
                        int z = Mth.floor(p.getZ());

                        List<CharmLease> entries = PedestalBlockEntity.leases(level.dimension());
                        if (entries != null)
                            for (CharmLease entry : entries) {
                                if (Math.abs(entry.position().getX() - x) <= 100
                                        && Math.abs(entry.position().getZ() - z) <= 100) {
                                    if (entry.kind() == CharmKind.PROTECTION) repell = true;
                                    if (entry.kind() == CharmKind.METEORITE) strike = false;
                                }
                            }
                    }

                    if (strike) spawnMeteorAtPlayer(p, repell);
                }
            }
        }

        if (meteorShower > 0) {
            meteorShower--;
        }

        if (meteorRand.nextInt(WorldData.METEOR_STRIKE_CHANCE.get() * 100) == 0
                && WorldData.ENABLE_METEOR_SHOWERS.get()) {
            meteorShower =
                    (int)
                            (WorldData.METEOR_SHOWER_DURATION.get() * 0.75
                                    + WorldData.METEOR_SHOWER_DURATION.get()
                                            * 0.25
                                            * meteorRand.nextFloat());
        }
    }

    public static void spawnMeteorAtPlayer(Player player, boolean repell) {

        MeteorEntity meteor = new MeteorEntity(player.level());
        double meteorX = player.getX() + meteorRand.nextInt(201) - 100;
        double meteorZ = player.getZ() + meteorRand.nextInt(201) - 100;
        meteor.snapTo(
                meteorX,
                DropHeight.clearOfTerrain(player.level(), 384, meteorX, meteorZ),
                meteorZ,
                0F,
                0F);

        Vec3 vec;
        if (repell) {
            vec =
                    new Vec3(meteor.getX() - player.getX(), 0, meteor.getZ() - player.getZ())
                            .normalize();
            double vel = meteorRand.nextDouble();
            vec = new Vec3(vec.x * vel, vec.y, vec.z * vel);
            meteor.safe = true;
        } else {
            vec =
                    new Vec3(meteorRand.nextDouble() - 0.5D, 0, 0)
                            .yRot((float) (Math.PI * meteorRand.nextDouble()));
        }

        meteor.setDeltaMovement(vec.x, -2.5D, vec.z);
        player.level().addFreshEntity(meteor);
    }
}
