// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrenda;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class TileEntityGlyphidSpawner extends BlockEntity {

    private record SpawnCandidate(
            Function<Level, @Nullable EntityGlyphid> factory, Supplier<int[]> chance) {}

    private static final List<SpawnCandidate> SPAWN_MAP =
            List.of(
                    new SpawnCandidate(
                            level ->
                                    EntityGlyphidBrenda.GRUNT_TYPE != null
                                            ? new EntityGlyphid(
                                                    EntityGlyphidBrenda.GRUNT_TYPE.get(), level)
                                            : null,
                            () -> MobData.glyphidChance()),
                    new SpawnCandidate(
                            level ->
                                    EntityGlyphidBombardier.TYPE != null
                                            ? new EntityGlyphidBombardier(
                                                    EntityGlyphidBombardier.TYPE.get(), level)
                                            : null,
                            () -> MobData.bombardierChance()),
                    new SpawnCandidate(
                            level ->
                                    EntityGlyphidBrawler.TYPE != null
                                            ? new EntityGlyphidBrawler(
                                                    EntityGlyphidBrawler.TYPE.get(), level)
                                            : null,
                            () -> MobData.brawlerChance()),
                    new SpawnCandidate(
                            level ->
                                    EntityGlyphidDigger.TYPE != null
                                            ? new EntityGlyphidDigger(
                                                    EntityGlyphidDigger.TYPE.get(), level)
                                            : null,
                            () -> MobData.diggerChance()),
                    new SpawnCandidate(
                            level ->
                                    EntityGlyphidBlaster.TYPE != null
                                            ? new EntityGlyphidBlaster(
                                                    EntityGlyphidBlaster.TYPE.get(), level)
                                            : null,
                            () -> MobData.blasterChance()),
                    new SpawnCandidate(
                            level ->
                                    new EntityGlyphidBehemoth(
                                            ModEntities.GLYPHID_BEHEMOTH.get(), level),
                            () -> MobData.behemothChance()),
                    new SpawnCandidate(
                            level ->
                                    new EntityGlyphidBrenda(
                                            ModEntities.GLYPHID_BRENDA.get(), level),
                            () -> MobData.brendaChance()),
                    new SpawnCandidate(
                            level ->
                                    new EntityGlyphidNuclear(
                                            ModEntities.GLYPHID_NUCLEAR.get(), level),
                            () -> MobData.johnsonChance()));

    private boolean initialSpawn = true;

    public TileEntityGlyphidSpawner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLYPHID_SPAWNER.get(), pos, state);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!initialSpawn && serverLevel.getGameTime() % MobData.swarmCooldown() != 0) return;

        initialSpawn = false;

        int count = 0;
        for (Entity e : serverLevel.getAllEntities()) {
            if (e instanceof EntityGlyphid) {
                count++;
                if (count >= MobData.SPAWN_MAX.get()) return;
            }
        }

        BlockPos pos = getBlockPos();
        List<EntityGlyphid> nearby =
                serverLevel.getEntitiesOfClass(
                        EntityGlyphid.class,
                        new AABB(
                                pos.getX() - 5,
                                pos.getY() + 1,
                                pos.getZ() - 5,
                                pos.getX() + 6,
                                pos.getY() + 7,
                                pos.getZ() + 6));

        float soot = PollutionHandler.getPollution(serverLevel, pos, PollutionType.SOOT);
        int subtype = subtype(getBlockState());

        if (nearby.size() <= 3 || subtype == EntityGlyphid.TYPE_RADIOACTIVE) {
            for (EntityGlyphid glyphid : createSwarm(serverLevel, soot, subtype)) {
                trySpawnEntity(serverLevel, pos, glyphid);
            }

            if (!initialSpawn
                    && serverLevel.getRandom().nextInt(MobData.scoutSwarmSpawnChance() + 1) == 0
                    && soot >= MobData.scoutThreshold()
                    && subtype != EntityGlyphid.TYPE_RADIOACTIVE) {

                EntityGlyphidScout scout =
                        new EntityGlyphidScout(ModEntities.GLYPHID_SCOUT.get(), serverLevel);
                if (subtype == EntityGlyphid.TYPE_INFECTED)
                    scout.setSubtype(EntityGlyphid.TYPE_INFECTED);
                trySpawnEntity(serverLevel, pos, scout);
            }
        }
    }

    private static int subtype(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.GLYPHID_SPAWNER_INFESTED.get()) return EntityGlyphid.TYPE_INFECTED;
        if (block == ModBlocks.GLYPHID_SPAWNER_RAD.get()) return EntityGlyphid.TYPE_RADIOACTIVE;
        return EntityGlyphid.TYPE_NORMAL;
    }

    private List<EntityGlyphid> createSwarm(ServerLevel level, float soot, int subtype) {
        RandomSource rand = level.getRandom();
        List<EntityGlyphid> currentSpawns = new ArrayList<>();
        int swarmAmount =
                (int)
                        Math.min(
                                MobData.BASE_SWARM_SIZE.get()
                                        * Math.max(
                                                MobData.SWARM_SCALING_MULT.get()
                                                        * (soot / MobData.SOOT_STEP.get()),
                                                1),
                                10);
        int cap = 100;

        while (currentSpawns.size() <= swarmAmount && cap >= 0) {
            for (SpawnCandidate candidate : SPAWN_MAP) {
                int[] chance = candidate.chance().get();
                int adjustedChance =
                        (int) (chance[0] + (chance[1] - chance[1] / Math.max(((soot + 1) / 3), 1)));
                if (soot >= chance[2] && rand.nextInt(100) <= adjustedChance) {
                    EntityGlyphid entity = candidate.factory().apply(level);
                    if (entity == null) continue;
                    if (subtype == EntityGlyphid.TYPE_INFECTED)
                        entity.setSubtype(EntityGlyphid.TYPE_INFECTED);
                    if (subtype == EntityGlyphid.TYPE_RADIOACTIVE)
                        entity.setSubtype(EntityGlyphid.TYPE_RADIOACTIVE);
                    currentSpawns.add(entity);
                }
            }
            cap--;
        }
        return currentSpawns;
    }

    private void trySpawnEntity(ServerLevel level, BlockPos pos, EntityGlyphid glyphid) {
        double offsetX = glyphid.getRandom().nextGaussian() * 3;
        double offsetZ = glyphid.getRandom().nextGaussian() * 3;

        for (int i = 0; i < 7; i++) {
            glyphid.snapTo(
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() - 2 + i,
                    pos.getZ() + 0.5 + offsetZ,
                    level.getRandom().nextFloat() * 360.0F,
                    0.0F);
            if (canSpawnAtCurrentPosition(level, glyphid)) {
                Services.PLATFORM.finalizeSpawn(
                        glyphid,
                        level,
                        level.getCurrentDifficultyAt(glyphid.blockPosition()),
                        EntitySpawnReason.SPAWNER,
                        null);
                level.addFreshEntity(glyphid);
                return;
            }
        }
    }

    private static boolean canSpawnAtCurrentPosition(ServerLevel level, EntityGlyphid glyphid) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.noCollision(glyphid, glyphid.getBoundingBox())
                && !level.containsAnyLiquid(glyphid.getBoundingBox());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("initialSpawn", initialSpawn);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        initialSpawn = input.getBooleanOr("initialSpawn", false);
    }
}
