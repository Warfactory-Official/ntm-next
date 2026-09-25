// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.injected.IClientImpactState;
import com.hbm.packet.toclient.ImpactSyncPayload;
import com.hbm.platform.Services;
import com.hbm.saveddata.TomSaveData;
import com.hbm.util.ChunkUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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

    private static final float SETTLE = 1F / 14400000F;
    private static final float COOL = 1F / 24000F;

    private static final List<LevelChunk> LOADED = new ArrayList<>();
    private static final Map<UUID, Snapshot> SENT = new HashMap<>();

    private ImpactWorldHandler() {}

    public static void init() {
        Services.SERVER.onServerTickPre(ImpactWorldHandler::onServerTick);
        Services.SERVER.onPlayerJoin(player -> SENT.remove(player.getUUID()));
        Services.SERVER.onPlayerRespawn(player -> SENT.remove(player.getUUID()));
        Services.SERVER.onPlayerChangeLevel(
                (player, origin, destination) -> SENT.remove(player.getUUID()));
        Services.SERVER.onPlayerDisconnect(player -> SENT.remove(player.getUUID()));
        Services.SERVER.onServerStopping(server -> SENT.clear());
    }

    private static void onServerTick(MinecraftServer server) {

        for (ServerLevel level : server.getAllLevels()) {
            TomSaveData data = TomSaveData.getExisting(level);
            if (data == null) continue;
            impactEffects(level, data);
            settle(data);
            if (level.dimension() == Level.OVERWORLD && data.fire > 0 && data.dust < 0.75F)
                burnExposed(level);
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TomSaveData data = TomSaveData.getExisting(player.level());
            Snapshot current =
                    data == null ? new Snapshot(0F, 0F) : new Snapshot(data.fire, data.dust);
            if (current.equals(SENT.put(player.getUUID(), current))) continue;
            Services.NETWORK.sendTo(new ImpactSyncPayload(current.fire, current.dust), player);
        }
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

    private static void burnExposed(ServerLevel level) {
        List<LivingEntity> living = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity alive) living.add(alive);
        }
        for (LivingEntity entity : living) burnIfExposed(level, entity);
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

        if (Services.PLATFORM.isFlammable(level, pos, state, Direction.UP)
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

        if (level.dimension() == Level.OVERWORLD
                && (mob.getBbHeight() >= 0.85F
                        || mob.getBbWidth() >= 0.85F && !water && !mob.isBaby())) {
            return true;
        }
        return water && level.getRandom().nextInt(5) != 0;
    }

    private record Snapshot(float fire, float dust) {}

    public static void receive(Player player, float fire, float dust) {
        ((IClientImpactState) player.level()).hbm$setImpactState(fire, dust);
    }

    public static float getFireForClient(Level level) {
        return level instanceof IClientImpactState state ? state.hbm$impactFire() : 0F;
    }

    public static float getDustForClient(Level level) {
        return level instanceof IClientImpactState state ? state.hbm$impactDust() : 0F;
    }
}
