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

package com.hbm.entity.item;

import com.hbm.blocks.machine.LightBeamSource;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.ModEntities;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FallingBlockBatchLayout;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.platform.Network;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class FallingBlockBatchEntity extends Entity
        implements FallingBlockBatchLayout.Children {
    private static final double GRAVITY = 0.04;
    private static final double DRAG = 0.98F;
    private static final int INITIAL_CAPACITY = 16;
    public static final int MAX_CHILDREN = ClientEventConstants.FALLING_BLOCK_BATCH_MAX_CHILDREN;

    private long[] sources = new long[INITIAL_CAPACITY];
    private BlockState[] states = new BlockState[INITIAL_CAPACITY];
    private double[] ys = new double[INITIAL_CAPACITY];
    private double[] velocities = new double[INITIAL_CAPACITY];
    private double[] fallDistances = new double[INITIAL_CAPACITY];
    private int[] ages = new int[INITIAL_CAPACITY];
    private int[] ids = new int[INITIAL_CAPACITY];
    private int size;
    private int nextId;
    private int revision;
    private int clientVisualRevision;
    private boolean running;
    private @Nullable Int2ObjectOpenHashMap<FallingBlockNTEntity> clientFallers;
    private @Nullable Int2ObjectOpenHashMap<FallingBlockNTEntity> previousFallers;
    private final LevelChunk[] nearbyChunks = new LevelChunk[9];
    private final BlockPos.MutableBlockPos probePos = new BlockPos.MutableBlockPos();

    private record Realification(FallingBlockNTEntity entity, boolean tickNow) {}

    public FallingBlockBatchEntity(
            EntityType<? extends FallingBlockBatchEntity> type, Level level) {
        super(type, level);
    }

    public static boolean eligible(BlockState state) {
        return !state.isAir()
                && !(state.getBlock() instanceof LightBeamSource)
                && !(state.getBlock() instanceof ConcretePowderBlock)
                && !state.hasBlockEntity();
    }

    public static FallingBlockBatchEntity create(ServerLevel level, LevelChunk chunk) {
        FallingBlockBatchEntity batch =
                new FallingBlockBatchEntity(ModEntities.FALLING_BLOCK_BATCH.get(), level);
        batch.setPos(
                chunk.getPos().getMinBlockX() + 8.0,
                level.getMinY(),
                chunk.getPos().getMinBlockZ() + 8.0);
        return batch;
    }

    public static void freezeChunk(ServerLevel level, ChunkPos pos) {
        int x = pos.getMinBlockX();
        int z = pos.getMinBlockZ();
        for (FallingBlockBatchEntity batch :
                level.getEntitiesOfClass(
                        FallingBlockBatchEntity.class,
                        new AABB(x, level.getMinY(), z, x + 16, level.getMaxY(), z + 16))) {
            if (batch.running) {
                batch.running = false;
                batch.sendSnapshot();
            }
        }
    }

    public void add(BlockPos source, BlockState state) {
        assert !level().isClientSide() && eligible(state);
        if (size == MAX_CHILDREN)
            throw new IllegalStateException("Falling block batch exceeded " + MAX_CHILDREN);
        if (size == sources.length) grow(size << 1);
        int i = size++;
        sources[i] = source.asLong();
        states[i] = state;
        ys[i] = source.getY() + 0.5;
        velocities[i] = 0.0;
        fallDistances[i] = 0.0;
        ages[i] = 0;
        ids[i] = nextId++;
    }

    public int fallerCount() {
        return size;
    }

    public Int2ObjectOpenHashMap<FallingBlockNTEntity> clientFallers() {
        if (clientFallers == null) clientFallers = new Int2ObjectOpenHashMap<>();
        return clientFallers;
    }

    public int revision() {
        return level().isClientSide() ? clientVisualRevision : revision;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            if (running)
                for (FallingBlockNTEntity proxy : clientFallers().values())
                    proxy.advanceVirtualVisual();
            return;
        }
        boolean resumed = !running;
        running = true;
        ServerLevel server = (ServerLevel) level();
        int chunkX = Mth.floor(getX()) >> 4;
        int chunkZ = Mth.floor(getZ()) >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                nearbyChunks[(dx + 1) * 3 + dz + 1] =
                        MultiblockCoreBlock.readableChunk(
                                server, (chunkX + dx) << 4, (chunkZ + dz) << 4);
            }
        }
        List<Entity> collidables = collidableEntities(server, chunkX, chunkZ);
        List<Realification> reify = null;
        boolean changed = false;
        for (int i = 0; i < size; ) {
            if (ages[i] == 0) {
                BlockPos source = BlockPos.of(sources[i]);
                FallingBlockNTEntity first = FallingBlockNTEntity.create(server, source, states[i]);
                first.dropItem = false;
                boolean freeAir = safe(server, i, true, chunkX, chunkZ, collidables, first);
                if (!freeAir && !simpleLanding(server, i, chunkX, chunkZ, collidables, first)) {
                    if (reify == null) reify = new ArrayList<>();
                    reify.add(new Realification(first, true));
                    remove(i);
                    changed = true;
                    continue;
                }
                server.tickNonPassenger(first);
                collidables = collidableEntities(server, chunkX, chunkZ);
                if (!first.isAlive()) {
                    remove(i);
                    changed = true;
                    continue;
                }
                ys[i] = first.getY();
                velocities[i] = first.getDeltaMovement().y;
                fallDistances[i] = first.fallDistance;
                ages[i] = first.virtualAge();
                if (first.onGround()
                        || first.getDeltaMovement().x != 0.0
                        || first.getDeltaMovement().z != 0.0
                        || Math.abs(first.getY() - (source.getY() + 0.46)) >= 1.0E-8) {
                    if (reify == null) reify = new ArrayList<>();
                    reify.add(new Realification(first, false));
                    remove(i);
                    changed = true;
                    continue;
                }
                i++;
                continue;
            }
            if (!safe(server, i, false, chunkX, chunkZ, collidables, null)) {
                BlockPos source = BlockPos.of(sources[i]);
                FallingBlockNTEntity falling =
                        FallingBlockNTEntity.resume(
                                server,
                                source,
                                states[i],
                                ys[i],
                                velocities[i],
                                fallDistances[i],
                                ages[i]);
                if (simpleLanding(server, i, chunkX, chunkZ, collidables, falling)) {
                    double expectedY = ys[i] + velocities[i] - GRAVITY;
                    server.tickNonPassenger(falling);
                    collidables = collidableEntities(server, chunkX, chunkZ);
                    if (!falling.isAlive()) {
                        remove(i);
                        changed = true;
                        continue;
                    }
                    if (!falling.onGround()
                            && falling.getDeltaMovement().x == 0.0
                            && falling.getDeltaMovement().z == 0.0
                            && Math.abs(falling.getY() - expectedY) < 1.0E-8) {
                        ys[i] = falling.getY();
                        velocities[i] = falling.getDeltaMovement().y;
                        fallDistances[i] = falling.fallDistance;
                        ages[i] = falling.virtualAge();
                        i++;
                        continue;
                    }
                    if (reify == null) reify = new ArrayList<>();
                    reify.add(new Realification(falling, false));
                } else {
                    if (reify == null) reify = new ArrayList<>();
                    reify.add(new Realification(falling, true));
                }
                remove(i);
                changed = true;
                continue;
            }
            double movement = velocities[i] - GRAVITY;
            ys[i] += movement;
            velocities[i] = movement * DRAG;
            fallDistances[i] -= (float) movement;
            ages[i]++;
            i++;
        }
        if (changed || resumed || tickCount % 20 == 0) sendSnapshot();
        if (reify != null)
            for (Realification result : reify) {
                FallingBlockNTEntity falling = result.entity();
                if (!server.addFreshEntity(falling))
                    throw new IllegalStateException("Could not materialize fallout faller");
                if (result.tickNow()) server.tickNonPassenger(falling);
            }
        if (size == 0) discard();
    }

    private boolean safe(
            ServerLevel level,
            int i,
            boolean firstTick,
            int chunkX,
            int chunkZ,
            List<Entity> collidables,
            @Nullable FallingBlockNTEntity probe) {
        if (ages[i] >= 600) return false;
        long source = sources[i];
        int x = BlockPos.getX(source);
        int z = BlockPos.getZ(source);
        double nextY = ys[i] + velocities[i] - GRAVITY;
        double minY = Math.min(ys[i], nextY) - 0.49;
        double maxY = Math.max(ys[i], nextY) + 0.49;
        if (minY <= level.getMinY() || maxY >= level.getMaxY()) return false;
        CollisionContext collision = null;
        for (int bx = x - 1; bx <= x + 1; bx++) {
            for (int bz = z - 1; bz <= z + 1; bz++) {
                int index = ((bx >> 4) - chunkX + 1) * 3 + ((bz >> 4) - chunkZ + 1);
                LevelChunk chunk = nearbyChunks[index];
                if (chunk == null || !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL))
                    return false;
                for (int by = Mth.floor(minY); by <= Mth.floor(maxY); by++) {
                    probePos.set(bx, by, bz);
                    BlockState nearby = chunk.getBlockState(probePos);
                    if (nearby.isAir()) continue;
                    if (bx == x
                            && bz == z
                            && firstTick
                            && by == BlockPos.getY(source)
                            && nearby.getBlock() == states[i].getBlock()) continue;
                    if (bx != x || bz != z) {
                        if (nearby.isCollisionShapeFullBlock(level, probePos)) continue;
                        if (collision == null) {
                            if (probe == null)
                                probe =
                                        FallingBlockNTEntity.resume(
                                                level,
                                                BlockPos.of(source),
                                                states[i],
                                                ys[i],
                                                velocities[i],
                                                fallDistances[i],
                                                ages[i]);
                            collision = CollisionContext.of(probe);
                        }
                        VoxelShape shape = nearby.getCollisionShape(level, probePos, collision);
                        if (missesColumn(shape, bx, bz, x, z)) continue;
                    }
                    return false;
                }
            }
        }
        if (collidables.isEmpty()) return true;
        AABB swept = new AABB(x + 0.01, minY, z + 0.01, x + 0.99, maxY, z + 0.99).inflate(1.0E-7);
        for (Entity entity : collidables) {
            if (entity.getBoundingBox().intersects(swept)) return false;
        }
        return true;
    }

    private static List<Entity> collidableEntities(ServerLevel level, int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        return level.getEntities(
                (Entity) null,
                new AABB(
                        minX - 1, level.getMinY(), minZ - 1, minX + 17, level.getMaxY(), minZ + 17),
                EntitySelector.CAN_BE_COLLIDED_WITH);
    }

    private static boolean missesColumn(VoxelShape shape, int bx, int bz, int x, int z) {
        if (shape.isEmpty()) return true;
        return bx + shape.max(Direction.Axis.X) <= x + 0.01
                || bx + shape.min(Direction.Axis.X) >= x + 0.99
                || bz + shape.max(Direction.Axis.Z) <= z + 0.01
                || bz + shape.min(Direction.Axis.Z) >= z + 0.99;
    }

    private boolean simpleLanding(
            ServerLevel level,
            int i,
            int chunkX,
            int chunkZ,
            List<Entity> collidables,
            FallingBlockNTEntity probe) {
        int x = BlockPos.getX(sources[i]);
        int z = BlockPos.getZ(sources[i]);
        double nextY = ys[i] + velocities[i] - GRAVITY;
        double minY = Math.min(ys[i], nextY) - 0.49;
        double maxY = Math.max(ys[i], nextY) + 0.49;
        boolean support = false;
        CollisionContext collision = null;
        for (int bx = x - 1; bx <= x + 1; bx++) {
            for (int bz = z - 1; bz <= z + 1; bz++) {
                int index = ((bx >> 4) - chunkX + 1) * 3 + ((bz >> 4) - chunkZ + 1);
                LevelChunk chunk = nearbyChunks[index];
                if (chunk == null || !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL))
                    return false;
                for (int by = Mth.floor(minY); by <= Mth.floor(maxY); by++) {
                    probePos.set(bx, by, bz);
                    BlockState nearby = chunk.getBlockState(probePos);
                    if (nearby.isAir()) continue;
                    if (bx == x && bz == z) {
                        if (!nearby.isCollisionShapeFullBlock(level, probePos)
                                || !nearby.getFluidState().isEmpty()
                                || nearby.hasBlockEntity()) return false;
                        support = true;
                    } else if (!nearby.isCollisionShapeFullBlock(level, probePos)) {
                        if (collision == null) collision = CollisionContext.of(probe);
                        VoxelShape shape = nearby.getCollisionShape(level, probePos, collision);
                        if (!missesColumn(shape, bx, bz, x, z)) return false;
                    }
                }
            }
        }
        if (!support) return false;
        if (collidables.isEmpty()) return true;
        AABB swept = new AABB(x + 0.01, minY, z + 0.01, x + 0.99, maxY, z + 0.99).inflate(1.0E-7);
        for (Entity entity : collidables) {
            if (entity.getBoundingBox().intersects(swept)) return false;
        }
        return true;
    }

    private void grow(int capacity) {
        sources = Arrays.copyOf(sources, capacity);
        states = Arrays.copyOf(states, capacity);
        ys = Arrays.copyOf(ys, capacity);
        velocities = Arrays.copyOf(velocities, capacity);
        fallDistances = Arrays.copyOf(fallDistances, capacity);
        ages = Arrays.copyOf(ages, capacity);
        ids = Arrays.copyOf(ids, capacity);
    }

    private void remove(int i) {
        int last = --size;
        sources[i] = sources[last];
        states[i] = states[last];
        ys[i] = ys[last];
        velocities[i] = velocities[last];
        fallDistances[i] = fallDistances[last];
        ages[i] = ages[last];
        ids[i] = ids[last];
        states[last] = null;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        PacketWire.sendTo(snapshot(), player);
    }

    private void sendSnapshot() {
        var watchers = Network.playersWatchingEntity(this);
        if (watchers.isEmpty()) return;
        revision++;
        PacketWire.sendToPlayers(snapshot(), watchers);
    }

    public EncodedPayload snapshot() {
        return FallingBlockBatchLayout.encode(getId(), revision, running, this);
    }

    @Override
    public int count() {
        return size;
    }

    @Override
    public int id(int index) {
        return ids[index];
    }

    @Override
    public long source(int index) {
        return sources[index];
    }

    @Override
    public int stateId(int index) {
        return Block.BLOCK_STATE_REGISTRY.getId(states[index]);
    }

    @Override
    public double y(int index) {
        return ys[index];
    }

    @Override
    public double velocity(int index) {
        return velocities[index];
    }

    @Override
    public int age(int index) {
        return ages[index];
    }

    public boolean beginNetworkSnapshot(int receivedRevision) {
        if (receivedRevision < revision) return false;
        previousFallers = clientFallers();
        clientFallers = new Int2ObjectOpenHashMap<>(previousFallers.size());
        return true;
    }

    public void acceptNetworkChild(
            int id, long source, int stateId, double y, double velocity, int age) {
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(stateId);
        FallingBlockNTEntity proxy = previousFallers.get(id);
        if (proxy == null || proxy.getBlockState() != state) {
            proxy =
                    FallingBlockNTEntity.resume(
                            level(), BlockPos.of(source), state, y, velocity, 0.0, age);
        } else {
            proxy.syncVirtualVisual(y, velocity, age);
        }
        clientFallers().put(id, proxy);
    }

    public void endNetworkSnapshot(int receivedRevision, boolean receivedRunning) {
        revision = receivedRevision;
        running = receivedRunning;
        previousFallers = null;
        clientVisualRevision++;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        CompoundTag data = input.read("fallers", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        nextId = data.getIntOr("next", 0);
        ListTag list = data.getListOrEmpty("records");
        if (list.size() > MAX_CHILDREN)
            throw new IllegalStateException("Oversized falling block batch " + list.size());
        if (list.size() > sources.length) grow(Math.max(INITIAL_CAPACITY, list.size()));
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElseThrow();
            sources[i] = entry.getLong("source").orElseThrow();
            CompoundTag stateTag = entry.getCompound("state").orElseThrow();
            states[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, stateTag);
            if (!NbtUtils.writeBlockState(states[i]).equals(stateTag)) {
                throw new IllegalStateException(
                        "Invalid batched faller block state at " + BlockPos.of(sources[i]));
            }
            ys[i] = entry.getDouble("y").orElseThrow();
            velocities[i] = entry.getDouble("velocity").orElseThrow();
            fallDistances[i] = entry.getDouble("fallDistance").orElseThrow();
            ages[i] = entry.getInt("age").orElseThrow();
            ids[i] = entry.getInt("id").orElseThrow();
        }
        size = list.size();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        CompoundTag data = new CompoundTag();
        data.putInt("next", nextId);
        ListTag list = new ListTag();
        for (int i = 0; i < size; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("source", sources[i]);
            entry.put("state", NbtUtils.writeBlockState(states[i]));
            entry.putDouble("y", ys[i]);
            entry.putDouble("velocity", velocities[i]);
            entry.putDouble("fallDistance", fallDistances[i]);
            entry.putInt("age", ages[i]);
            entry.putInt("id", ids[i]);
            list.add(entry);
        }
        data.put("records", list);
        output.store("fallers", CompoundTag.CODEC, data);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
