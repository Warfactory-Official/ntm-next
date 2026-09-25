// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.interfaces.IExplosionRay;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class ExplosionNukeRayBatched implements IExplosionRay {

    private static final float NUKE_RESISTANCE_CUTOFF = 2_000_000F;
    private static final float INITIAL_ENERGY_FACTOR = 0.3F;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    private final Level level;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final int strength;
    private final int radius;

    private final int minY;
    private final int maxY;
    private final int worldHeight;

    private final Long2ObjectOpenHashMap<BitSet> perChunk = new Long2ObjectOpenHashMap<>();
    private final int gspNumMax;
    private long[] orderedChunks = new long[0];
    private int orderedIndex = 0;
    private int gspNum;
    private double gspX;
    private double gspY;

    private boolean cacheComplete = false;
    private boolean isContained = true;

    private int destructBit = -1;
    private BitSet destructArray = null;

    @SuppressWarnings("unused")
    private UUID detonator;

    public ExplosionNukeRayBatched(Level level, int x, int y, int z, int strength, int radius) {
        this.level = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.radius = radius;

        this.minY = level.getMinY();
        this.maxY = level.getMaxY();
        this.worldHeight = level.getHeight();

        this.gspNumMax = (int) (2.5 * Math.PI * strength * strength);
        this.gspNum = 1;
        this.gspX = Math.PI;
        this.gspY = 0.0;
    }

    public static float getNukeResistance(BlockState state) {
        if (state.liquid()) return 0.1F;
        if (state.is(Blocks.SANDSTONE)) return Blocks.STONE.getExplosionResistance();
        if (state.is(Blocks.OBSIDIAN)) return Blocks.STONE.getExplosionResistance() * 3.0F;
        return state.getBlock().getExplosionResistance();
    }

    private void generateGspUp() {
        if (this.gspNum < this.gspNumMax) {
            int k = this.gspNum + 1;
            double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
            this.gspX = Math.acos(hk);
            double prevLon = this.gspY;
            double lon = prevLon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
            this.gspY = lon % (Math.PI * 2);
        } else {
            this.gspX = 0.0;
            this.gspY = 0.0;
        }
        this.gspNum++;
    }

    private Vec3 sphericalToCartesian() {
        double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
        double dy = Math.sin(this.gspX) * Math.sin(this.gspY);
        double dz = Math.cos(this.gspX);
        return new Vec3(dx, dy, dz);
    }

    private void addPos(int x, int y, int z) {
        long key = ChunkPos.pack(x >> 4, z >> 4);
        BitSet hits = perChunk.get(key);
        if (hits == null) {
            hits = new BitSet(256 * worldHeight);
            perChunk.put(key, hits);
        }
        hits.set(bitIndex(x & 0xF, y, z & 0xF));
    }

    private int bitIndex(int localX, int y, int localZ) {
        return ((y - minY) << 8) | (localX << 4) | localZ;
    }

    private void cacheChunksTick(long timeMs) {
        long deadline = System.currentTimeMillis() + timeMs;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        while (this.gspNum <= this.gspNumMax) {
            Vec3 vec = sphericalToCartesian();

            if (Double.isNaN(vec.x) || Double.isNaN(vec.y) || Double.isNaN(vec.z)) {
                generateGspUp();
                continue;
            }
            float rayStrength = strength * INITIAL_ENERGY_FACTOR;

            for (int r = 0; r < radius + 1; r++) {
                int iY = (int) Math.floor(posY + vec.y * r);
                if (iY < minY || iY > maxY) {
                    isContained = false;
                    break;
                }

                int iX = (int) Math.floor(posX + vec.x * r);
                int iZ = (int) Math.floor(posZ + vec.z * r);

                pos.set(iX, iY, iZ);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock().getExplosionResistance() >= NUKE_RESISTANCE_CUTOFF) break;

                int r0 = r > 0 ? r : 1;
                rayStrength -=
                        (float) (Math.pow(getNukeResistance(state) + 1, 3.0 * r0 / radius) - 1.0);

                if (rayStrength > 0) {
                    if (!state.isAir()) addPos(iX, iY, iZ);
                    if (r >= radius) isContained = false;
                } else {
                    break;
                }
            }

            generateGspUp();
            if (System.currentTimeMillis() >= deadline) return;
        }

        orderChunks();
        cacheComplete = true;
    }

    private void orderChunks() {
        int chunkX = posX >> 4;
        int chunkZ = posZ >> 4;
        long[] keys = perChunk.keySet().toLongArray();
        long[] sortKeys = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            int dist =
                    Math.abs(chunkX - ChunkPos.getX(keys[i]))
                            + Math.abs(chunkZ - ChunkPos.getZ(keys[i]));
            sortKeys[i] = ((long) dist << 32) | i;
        }
        Arrays.sort(sortKeys);
        orderedChunks = new long[keys.length];
        for (int i = 0; i < keys.length; i++) orderedChunks[i] = keys[(int) sortKeys[i]];
        orderedIndex = 0;
    }

    private void destructionTick(long timeMs) {
        long deadline = System.currentTimeMillis() + timeMs;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ServerLevel server = (ServerLevel) level;

        while (orderedIndex < orderedChunks.length) {
            long key = orderedChunks[orderedIndex];
            if (destructArray == null) {
                destructArray = perChunk.get(key);
                destructBit = destructArray.nextSetBit(0);
            }

            int chunkX = ChunkPos.getX(key) << 4;
            int chunkZ = ChunkPos.getZ(key) << 4;

            LevelChunk chunk = level.getChunk(ChunkPos.getX(key), ChunkPos.getZ(key));

            int removed = 0;
            while (destructBit >= 0) {
                int bit = destructBit;
                int y = (bit >>> 8) + minY;
                int localX = (bit >>> 4) & 0xF;
                int localZ = bit & 0xF;
                pos.set(chunkX | localX, y, chunkZ | localZ);

                BlockState outgoing = chunk.getBlockState(pos);
                level.setBlock(pos, AIR, UPDATE_FLAGS);
                ChunkUtil.dispatchRemovalHook(server, pos, outgoing, AIR);
                destructBit = destructArray.nextSetBit(bit + 1);
                removed++;
                if ((removed & 0xFF) == 0 && System.currentTimeMillis() >= deadline) return;
            }

            perChunk.remove(key);
            destructArray = null;
            orderedIndex++;
        }
    }

    @Override
    public void update(long msBudget) {
        if (cacheComplete) destructionTick(msBudget);
        else cacheChunksTick(msBudget);
    }

    @Override
    public void cancel() {
        cacheComplete = true;
        perChunk.clear();
        orderedChunks = new long[0];
        orderedIndex = 0;
        destructArray = null;
        destructBit = -1;
    }

    @Override
    public boolean isComplete() {
        return cacheComplete && perChunk.isEmpty();
    }

    @Override
    public boolean isContained() {
        return isContained;
    }

    @Override
    public void setDetonator(UUID detonator) {
        this.detonator = detonator;
    }

    public Long2ObjectMap<BitSet> getPerChunk() {
        return perChunk;
    }
}
