// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.platform.FabricPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityGlyphidSpawner extends BlockEntity {
    private static final EntityTypeTest<Entity, EntityGlyphid> GLYPHIDS =
            EntityTypeTest.forClass(EntityGlyphid.class);

    private record SpawnCandidate(
            Supplier<EntityType<? extends EntityGlyphid>> type, Supplier<int[]> chance) {}

    private static final List<SpawnCandidate> SPAWN_MAP =
            List.of(
                    new SpawnCandidate(() -> ModEntities.GLYPHID.get(), MobData::glyphidChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_BOMBARDIER.get(), MobData::bombardierChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_BRAWLER.get(), MobData::brawlerChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_DIGGER.get(), MobData::diggerChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_BLASTER.get(), MobData::blasterChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_BEHEMOTH.get(), MobData::behemothChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_BRENDA.get(), MobData::brendaChance),
                    new SpawnCandidate(
                            () -> ModEntities.GLYPHID_NUCLEAR.get(), MobData::johnsonChance));

    private boolean initialSpawn = true;
    private int spawnDelay;

    public BlockEntityGlyphidSpawner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLYPHID_SPAWNER.get(), pos, state);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = getBlockPos();
        if (!server.isSpawnerBlockEnabled()) return;
        if (initialSpawn) {
            if (server.getDifficulty() == Difficulty.PEACEFUL) return;
            initialSpawn = false;
        } else {
            if (!server.hasNearbyAlivePlayer(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    MobData.REQUIRED_PLAYER_RANGE.get())) return;
            if (spawnDelay > 0) {
                spawnDelay--;
                server.blockEntityChanged(pos);
                return;
            }
            if (server.getDifficulty() == Difficulty.PEACEFUL) {
                delay(server, false);
                return;
            }
        }

        int limit = MobData.MAX_NEARBY_ENTITIES.get();
        int attempts = MobData.SPAWN_COUNT.get();
        if (limit == 0 || attempts == 0) {
            delay(server, true);
            return;
        }

        int range = MobData.SPAWN_RANGE.get();
        List<EntityGlyphid> nearby = new ArrayList<>();
        server.getEntities(
                GLYPHIDS,
                new AABB(pos).inflate(range),
                EntitySelector.NO_SPECTATORS,
                nearby,
                limit);
        int count = nearby.size();
        if (count >= limit) {
            delay(server, true);
            return;
        }

        float soot = PollutionHandler.getPollution(server, pos, PollutionType.SOOT);
        byte subtype = subtype(getBlockState());
        RandomSource random = server.getRandom();
        int[] weights = new int[SPAWN_MAP.size()];
        int total = 0;
        for (int i = 0; i < weights.length; i++) {
            int[] chance = SPAWN_MAP.get(i).chance().get();
            if (soot < chance[2]) continue;
            int adjusted =
                    (int) (chance[0] + (chance[1] - chance[1] / Math.max((soot + 1) / 3, 1)));
            weights[i] = Mth.clamp(adjusted, -1, 99) + 1;
            total += weights[i];
        }
        boolean scout =
                subtype != EntityGlyphid.TYPE_RADIOACTIVE
                        && soot >= MobData.scoutThreshold()
                        && random.nextInt(MobData.scoutSwarmSpawnChance() + 1) == 0;
        boolean spawned = false;
        for (int attempt = 0; attempt < attempts && count < limit; attempt++) {
            EntityType<? extends EntityGlyphid> type;
            if (scout && attempt == attempts - 1) {
                type = ModEntities.GLYPHID_SCOUT.get();
            } else {
                if (total == 0) continue;
                int choice = random.nextInt(total);
                int index = 0;
                while ((choice -= weights[index]) >= 0) index++;
                type = SPAWN_MAP.get(index).type().get();
            }
            double x = pos.getX() + (random.nextDouble() - random.nextDouble()) * range + 0.5;
            double y = pos.getY() + random.nextInt(3) - 1;
            double z = pos.getZ() + (random.nextDouble() - random.nextDouble()) * range + 0.5;
            if (!server.noCollision(type.getSpawnAABB(x, y, z))) continue;
            EntityGlyphid glyphid = type.create(server, EntitySpawnReason.SPAWNER);
            if (subtype != EntityGlyphid.TYPE_NORMAL) glyphid.setSubtype(subtype);
            glyphid.snapTo(x, y, z, random.nextFloat() * 360F, 0F);
            if (!server.noCollision(glyphid, glyphid.getBoundingBox())
                    || server.containsAnyLiquid(glyphid.getBoundingBox())) continue;
            FabricPlatform.finalizeSpawn(
                    glyphid,
                    server,
                    server.getCurrentDifficultyAt(glyphid.blockPosition()),
                    EntitySpawnReason.SPAWNER,
                    null);
            if (server.addFreshEntity(glyphid)) {
                count++;
                spawned = true;
            }
        }
        delay(server, spawned);
    }

    private void delay(ServerLevel server, boolean fullCooldown) {
        if (fullCooldown) {
            int minimum = MobData.MIN_SPAWN_DELAY.get();
            spawnDelay =
                    minimum
                            + server.getRandom()
                                    .nextInt(MobData.MAX_SPAWN_DELAY.get() - minimum + 1);
        } else {
            spawnDelay = MobData.FAILED_SPAWN_DELAY.get();
        }
        server.blockEntityChanged(worldPosition);
    }

    private static byte subtype(BlockState state) {
        if (state.is(ModBlocks.GLYPHID_SPAWNER_INFESTED.get())) return EntityGlyphid.TYPE_INFECTED;
        if (state.is(ModBlocks.GLYPHID_SPAWNER_RAD.get())) return EntityGlyphid.TYPE_RADIOACTIVE;
        return EntityGlyphid.TYPE_NORMAL;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("initial_spawn", initialSpawn);
        output.putInt("spawn_delay", spawnDelay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        initialSpawn = input.getBooleanOr("initial_spawn", true);
        spawnDelay = input.getIntOr("spawn_delay", 0);
        assert spawnDelay >= 0;
    }
}
