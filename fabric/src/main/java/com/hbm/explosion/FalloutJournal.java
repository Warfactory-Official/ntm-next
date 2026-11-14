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

package com.hbm.explosion;

import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.entity.effect.FalloutRainEntity;
import com.hbm.entity.effect.FalloutTerrain;
import com.hbm.entity.item.FallingMultiblockEntity;
import com.hbm.entity.item.PendingFallers;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.interfaces.injected.BulkLightEngine;
import com.hbm.interfaces.injected.ChunkExtension;
import com.hbm.platform.ServerEvents;
import com.hbm.util.BulkSectionUpdates;
import com.hbm.util.ChunkUtil;
import com.hbm.util.RegistryUtil;
import com.hbm.util.SectionSnapshot;
import com.hbm.world.biome.NtmBiomes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class FalloutJournal {
    private static final Identifier JOURNAL_ID = RegistryUtil.id("fallout_journal");
    private static final SavedDataType<Journal> TYPE =
            new SavedDataType<>(JOURNAL_ID, Journal::new, Journal.CODEC, null);
    private static final Map<ServerLevel, Runtime> RUNTIMES = new ConcurrentHashMap<>();
    private static final Map<RegionStorageInfo, Runtime> WRITES = new ConcurrentHashMap<>();
    private static final ThreadLocal<FalloutRainEntity.WorkerScratch> SCRATCH =
            ThreadLocal.withInitial(FalloutRainEntity.WorkerScratch::new);
    private static final VarHandle WORDS = MethodHandles.arrayElementVarHandle(long[].class);
    private static final Op[] NONE = new Op[0];

    private FalloutJournal() {}

    public static void init() {
        final Consumer<ServerLevel> callback =
                level -> {
                    Journal journal = level.getDataStorage().get(TYPE);
                    if (journal != null && !journal.operations.isEmpty())
                        startRuntime(level, journal).publishAll();
                };
        ServerLevelEvents.LOAD.register((server, level2) -> callback.accept(level2));
        final BiConsumer<ServerLevel, LevelChunk> callback2 = FalloutJournal::chunkReadable;
        ServerEvents.CHUNK_LOAD.add(callback2);
        final BiConsumer<ServerLevel, LevelChunk> callback3 =
                (level, chunk) -> {
                    Runtime runtime = RUNTIMES.get(level);
                    if (runtime != null) runtime.unloaded(chunk.getPos().pack());
                };
        ServerEvents.CHUNK_UNLOAD.add(callback3);
        final Consumer<MinecraftServer> callback4 =
                _ -> {
                    for (Runtime runtime : RUNTIMES.values()) runtime.tick();
                };
        ServerEvents.TICK_PRE.add(callback4);
        final Consumer<MinecraftServer> callback5 =
                _ -> {
                    for (Runtime runtime : RUNTIMES.values()) runtime.cancelJob();
                    RUNTIMES.clear();
                    WRITES.clear();
                };
        ServerEvents.STOPPING.add(callback5);
    }

    public static void chunkReadable(ServerLevel level, LevelChunk chunk) {
        Runtime runtime = RUNTIMES.get(level);
        if (runtime != null) runtime.loaded(chunk);
    }

    public static long record(
            ServerLevel level, double x, double z, int scale, Block layer, boolean biomes) {
        Journal journal = level.getDataStorage().computeIfAbsent(TYPE);
        long id = journal.nextId;
        journal.nextId = Math.incrementExact(id);
        journal.setDirty();
        Holder<Biome> inner = null, crater = null, outer = null;
        if (biomes && scale >= 25) {
            var registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
            inner = registry.getOrThrow(NtmBiomes.CRATER_INNER);
            crater = registry.getOrThrow(NtmBiomes.CRATER);
            outer = registry.getOrThrow(NtmBiomes.CRATER_OUTER);
        }
        Op op =
                Op.create(
                        id, x, z, scale, layer, inner, crater, outer, level.getRandom().nextLong());
        if (op.remaining == 0) return id;
        journal.operations.put(id, op);
        CompletableFuture<?> fence = level.getDataStorage().scheduleSave();
        Runtime runtime = RUNTIMES.get(level);
        Runtime owner = runtime != null ? runtime : startRuntime(level, journal);
        fence.thenRunAsync(() -> owner.publish(op), level.getServer());
        return id;
    }

    public static boolean settled(ServerLevel level, long id) {
        Journal journal = level.getDataStorage().get(TYPE);
        Op op = journal == null ? null : journal.operations.get(id);
        return op == null || op.published && op.resident.isEmpty();
    }

    public static boolean isJournal(SavedDataType<?> type) {
        return type.id().equals(JOURNAL_ID);
    }

    public static @Nullable SavedData readJournal(Path path, HolderLookup.Provider registries) {
        try {
            CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            return TYPE.codec()
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("data"))
                    .getOrThrow();
        } catch (NoSuchFileException absent) {
            return null;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static void writeJournal(CompoundTag tag, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Path next = path.resolveSibling(path.getFileName() + ".next");
            NbtIo.writeCompressed(tag, next);
            Files.move(
                    next,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static long persistedMarker(ServerLevel level, ChunkPos pos, long marker) {
        if (marker == 0) return 0;
        Runtime runtime = RUNTIMES.get(level);
        if (runtime == null) return 0;
        for (Op op : runtime.published) {
            if (op.id > marker) break;
            if (op.ring(pos.x(), pos.z()) != FalloutTerrain.NONE) return marker;
        }
        return 0;
    }

    public static void written(RegionStorageInfo info, ChunkPos pos, ByteBuffer bytes)
            throws IOException {
        Runtime runtime = WRITES.get(info);
        if (runtime == null || !runtime.pendingAt(pos.x(), pos.z())) return;
        ByteBuffer payload = bytes.duplicate();
        payload.position(5);
        var fields =
                new CollectFields(
                        new FieldSelector(
                                LongTag.TYPE, ChunkExtension.FALLOUT_APPLIED_THROUGH_NBT_KEY));
        try (var input =
                new DataInputStream(
                        RegionFileVersion.fromId(bytes.get(4))
                                .wrap(
                                        new InputStream() {
                                            @Override
                                            public int read() {
                                                return payload.hasRemaining()
                                                        ? Byte.toUnsignedInt(payload.get())
                                                        : -1;
                                            }

                                            @Override
                                            public int read(byte[] out, int offset, int length) {
                                                if (length == 0) return 0;
                                                if (!payload.hasRemaining()) return -1;
                                                int count = Math.min(length, payload.remaining());
                                                payload.get(out, offset, count);
                                                return count;
                                            }
                                        }))) {
            NbtIo.parse(input, fields, NbtAccounter.unlimitedHeap());
        }
        long marker =
                ((CompoundTag) fields.getResult())
                        .getLongOr(ChunkExtension.FALLOUT_APPLIED_THROUGH_NBT_KEY, 0);
        observeSave(runtime.level, pos, marker, CompletableFuture.completedFuture(null));
    }

    public static void observeSave(
            ServerLevel level, ChunkPos pos, long appliedThrough, CompletableFuture<Void> write) {
        if (appliedThrough == 0) return;
        write.thenRunAsync(
                () -> {
                    Runtime runtime = RUNTIMES.get(level);
                    if (runtime != null) runtime.acknowledge(pos.x(), pos.z(), appliedThrough);
                },
                level.getServer());
    }

    public static void applyParsed(ServerLevel level, SerializableChunkData record) {
        Runtime runtime = RUNTIMES.get(level);
        if (runtime == null) return;
        var data = (ChunkExtension) (Object) record;
        int chunkX = record.chunkPos().x();
        int chunkZ = record.chunkPos().z();
        List<Op> ops =
                applicable(runtime.published, chunkX, chunkZ, data.hbm$falloutAppliedThrough());
        if (ops.isEmpty()) return;
        var sections = new LevelChunkSection[level.getSectionsCount()];
        List<SerializableChunkData.SectionData> sectionData = record.sectionData();
        for (var section : sectionData) {
            int index = level.getSectionIndexFromSectionY(section.y());
            if (index >= 0 && index < sections.length && section.chunkSection() != null) {
                sections[index] = section.chunkSection();
            }
        }
        for (int index = 0; index < sections.length; index++) {
            if (sections[index] != null) continue;
            sections[index] = new LevelChunkSection(record.containerFactory());
            sectionData.add(
                    new SerializableChunkData.SectionData(
                            level.getSectionYFromSectionIndex(index), sections[index], null, null));
        }
        FalloutReplay replay = data.hbm$falloutReplay();
        if (replay == null) replay = new FalloutReplay(level.getMinY());
        var written = new Long2ObjectOpenHashMap<BlockState>();
        var fallers = new Long2ObjectOpenHashMap<BlockState>();
        var surfaces = new LongOpenHashSet();
        convert(level, sections, chunkX, chunkZ, ops, replay.deferred, written, fallers, surfaces);
        for (var iterator = written.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            replay.changed.putIfAbsent(entry.getLongKey(), entry.getValue());
        }
        for (long cell : surfaces) {
            BlockState state = sectionState(level, sections, cell);
            if (MultiblockSurface.foldedCore(state) != null) {
                replay.cores.add(cell);
                continue;
            }
            int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
            long entry = MultiblockCoreIndex.lookup(data, level, x, y, z);
            if (entry != MultiblockCoreIndex.NO_ENTRY)
                replay.cores.add(MultiblockCoreIndex.corePacked(entry, x, y, z));
        }
        PendingFallers.appendAll(data, fallers);
        rescanHeightmaps(level, record.heightmaps(), sections, written);
        replay.parsedCells = new LongArrayList(written.keySet());
        replay.parsedOld = new ArrayList<>(written.values());
        data.hbm$setFalloutAppliedThrough(ops.getLast().id);
        if (replay.persistable() || !replay.parsedCells.isEmpty())
            data.hbm$setFalloutReplay(replay);
    }

    public static void afterRead(ServerLevel level, ChunkAccess chunk) {
        ChunkAccess target =
                chunk instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : chunk;
        FalloutReplay replay =
                ((com.hbm.interfaces.injected.ChunkExtension) (target)).hbm$falloutReplay();
        if (replay == null || replay.parsedCells == null) return;
        var pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < replay.parsedCells.size(); i++) {
            pos.set(replay.parsedCells.getLong(i));
            level.updatePOIOnBlockStateChange(
                    pos, replay.parsedOld.get(i), target.getBlockState(pos));
        }
        replay.lightChecks = replay.parsedCells.toLongArray();
        replay.parsedCells = null;
        replay.parsedOld = null;
        if (!replay.persistable() && replay.lightChecks.length == 0)
            ((com.hbm.interfaces.injected.ChunkExtension) (target)).hbm$setFalloutReplay(null);
    }

    public static @Nullable CompletableFuture<ChunkAccess> beforeLight(
            ServerLevel level, BulkLightEngine light, ChunkAccess chunk) {
        if (chunk instanceof ImposterProtoChunk imposter) {
            LevelChunk wrapped = imposter.getWrapped();
            FalloutReplay replay =
                    ((com.hbm.interfaces.injected.ChunkExtension) (wrapped)).hbm$falloutReplay();
            if (replay != null && replay.lightChecks != null) {
                light.hbm$checkBlocks(wrapped, replay.lightChecks);
                replay.lightChecks = null;
                if (!replay.persistable())
                    ((com.hbm.interfaces.injected.ChunkExtension) (wrapped))
                            .hbm$setFalloutReplay(null);
            }
            return null;
        }
        if (!(chunk instanceof ProtoChunk proto)) return null;
        Runtime runtime = RUNTIMES.get(level);
        if (runtime == null) return null;
        List<Op> ops =
                applicable(
                        runtime.published,
                        chunk.getPos().x(),
                        chunk.getPos().z(),
                        ((com.hbm.interfaces.injected.ChunkExtension) (proto))
                                .hbm$falloutAppliedThrough());
        if (ops.isEmpty()) return null;
        return CompletableFuture.supplyAsync(
                () -> {
                    convertProto(level, proto, ops);
                    return chunk;
                },
                Util.backgroundExecutor());
    }

    private static void convertProto(ServerLevel level, ProtoChunk proto, List<Op> ops) {
        int chunkX = proto.getPos().x();
        int chunkZ = proto.getPos().z();
        LevelChunkSection[] sections = proto.getSections();
        FalloutReplay replay =
                ((com.hbm.interfaces.injected.ChunkExtension) (proto)).hbm$falloutReplay();
        if (replay == null) replay = new FalloutReplay(level.getMinY());
        var copies = new LevelChunkSection[sections.length];
        for (int i = 0; i < sections.length; i++) copies[i] = sections[i].copy();
        var written = new Long2ObjectOpenHashMap<BlockState>();
        var fallers = new Long2ObjectOpenHashMap<BlockState>();
        var surfaces = new LongOpenHashSet();
        convert(level, copies, chunkX, chunkZ, ops, replay.deferred, written, fallers, surfaces);
        var pos = new BlockPos.MutableBlockPos();
        for (var iterator = written.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            pos.set(entry.getLongKey());
            BlockState target = sectionState(level, copies, entry.getLongKey());
            proto.setBlockState(pos, target, 0);
            level.updatePOIOnBlockStateChange(pos, entry.getValue(), target);
            replay.changed.putIfAbsent(entry.getLongKey(), entry.getValue());
        }
        for (int i = 0; i < sections.length; i++) sections[i].biomes = copies[i].biomes;
        for (long cell : surfaces) {
            BlockState state = proto.getBlockState(pos.set(cell));
            if (MultiblockSurface.foldedCore(state) != null) {
                replay.cores.add(cell);
                continue;
            }
            int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
            long entry = MultiblockCoreIndex.lookup(proto, level, x, y, z);
            if (entry != MultiblockCoreIndex.NO_ENTRY)
                replay.cores.add(MultiblockCoreIndex.corePacked(entry, x, y, z));
        }
        PendingFallers.appendAll(proto, fallers);
        ((com.hbm.interfaces.injected.ChunkExtension) (proto))
                .hbm$setFalloutAppliedThrough(ops.getLast().id);
        if (replay.persistable())
            ((com.hbm.interfaces.injected.ChunkExtension) (proto)).hbm$setFalloutReplay(replay);
    }

    public static void replay(ServerLevel level, LevelChunk chunk) {
        FalloutReplay replay =
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$falloutReplay();
        if (replay == null || replay.changed.isEmpty() && replay.deferred.isEmpty()) return;
        var pos = new BlockPos.MutableBlockPos();
        var neighbour = new BlockPos.MutableBlockPos();
        for (var iterator = replay.changed.long2ObjectEntrySet().fastIterator();
                iterator.hasNext(); ) {
            var entry = iterator.next();
            pos.set(entry.getLongKey());
            BlockState old = entry.getValue();
            BlockState now = chunk.getBlockState(pos);
            if (now == old) continue;
            ChunkUtil.dispatchRemovalHook(level, pos, old, now);
            now.onPlace(level, pos, old, false);
            Block block = now.getBlock();
            for (Direction direction : NeighborUpdater.UPDATE_ORDER) {
                neighbour.setWithOffset(pos, direction);
                if (!replay.changed.containsKey(neighbour.asLong()))
                    level.neighborChanged(neighbour, block, null);
            }
            for (Direction direction : BlockBehaviour.UPDATE_SHAPE_ORDER) {
                neighbour.setWithOffset(pos, direction);
                if (!replay.changed.containsKey(neighbour.asLong())) {
                    level.neighborShapeChanged(
                            direction.getOpposite(),
                            neighbour,
                            pos,
                            now,
                            Block.UPDATE_CLIENTS,
                            512);
                }
            }
        }
        for (var iterator = replay.deferred.long2ObjectEntrySet().fastIterator();
                iterator.hasNext(); ) {
            var entry = iterator.next();
            ChunkUtil.replaceEmptied(
                    level, BlockPos.of(entry.getLongKey()), entry.getValue(), Block.UPDATE_ALL);
        }
        replay.changed.clear();
        replay.deferred.clear();
        if (replay.cores.isEmpty())
            ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$setFalloutReplay(null);
        chunk.markUnsaved();
    }

    public static void tickCores(ServerLevel level, LevelChunk chunk) {
        FalloutReplay replay =
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$falloutReplay();
        if (replay == null || replay.cores.isEmpty()) return;
        Runtime runtime = RUNTIMES.get(level);
        var pos = new BlockPos.MutableBlockPos();
        for (var iterator = replay.cores.iterator(); iterator.hasNext(); ) {
            long core = iterator.nextLong();
            if (!level.isPositionEntityTicking(pos.set(core))) continue;
            iterator.remove();
            if ((runtime == null || runtime.evaluatedCores.add(core))
                    && MultiblockSurface.foldedCore(level.getBlockState(pos)) != null) {
                FallingMultiblockEntity.tryFall(level, pos.immutable());
            }
        }
        if (!replay.persistable())
            ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$setFalloutReplay(null);
        chunk.markUnsaved();
    }

    private static List<Op> applicable(Op[] published, int chunkX, int chunkZ, long marker) {
        List<Op> ops = List.of();
        for (Op op : published) {
            if (op.id <= marker || op.ring(chunkX, chunkZ) == FalloutTerrain.NONE) continue;
            if (ops.isEmpty()) ops = new ArrayList<>(2);
            ops.add(op);
        }
        return ops;
    }

    private static void convert(
            ServerLevel level,
            LevelChunkSection[] sections,
            int chunkX,
            int chunkZ,
            List<Op> ops,
            Long2ObjectOpenHashMap<BlockState> deferred,
            Long2ObjectOpenHashMap<BlockState> written,
            Long2ObjectOpenHashMap<BlockState> fallers,
            LongOpenHashSet surfaces) {
        List<FalloutEntry> table = FalloutConfigJSON.entries(level.registryAccess());
        FalloutRainEntity.WorkerScratch scratch = SCRATCH.get();
        long key = ChunkPos.pack(chunkX, chunkZ);
        for (Op op : ops) {
            scratch.clear();
            FalloutTerrain.convert(
                    level,
                    sections,
                    deferred,
                    scratch,
                    chunkX,
                    chunkZ,
                    op.x,
                    op.z,
                    op.scale,
                    op.ring(chunkX, chunkZ) == FalloutTerrain.OUTER,
                    op.layer,
                    table,
                    FalloutTerrain.chunkSeed(op.seed, key));
            FalloutTerrain.dropUnsupportedLayers(
                    level, sections, deferred, scratch.updates, op.layer);
            ChunkUtil.applyToSections(
                    sections,
                    level.getMinSectionY(),
                    chunkX,
                    chunkZ,
                    scratch.updates,
                    written,
                    deferred,
                    true,
                    _ -> {});
            for (var iterator = scratch.spawnFalling.long2ObjectEntrySet().fastIterator();
                    iterator.hasNext(); ) {
                var entry = iterator.next();
                fallers.putIfAbsent(entry.getLongKey(), entry.getValue());
            }
            surfaces.addAll(scratch.fallingSurfaceCells);
            if (op.inner != null) {
                FalloutTerrain.repaintBiomes(
                        sections,
                        chunkX,
                        chunkZ,
                        op.x,
                        op.z,
                        op.scale,
                        op.inner,
                        op.crater,
                        op.outer,
                        _ -> {});
            }
        }
    }

    private static BlockState sectionState(
            ServerLevel level, LevelChunkSection[] sections, long pos) {
        int y = BlockPos.getY(pos);
        return sections[level.getSectionIndex(y)].getBlockState(
                BlockPos.getX(pos) & 15, y & 15, BlockPos.getZ(pos) & 15);
    }

    private static void rescanHeightmaps(
            ServerLevel level,
            Map<Heightmap.Types, long[]> heightmaps,
            LevelChunkSection[] sections,
            Long2ObjectOpenHashMap<BlockState> written) {
        if (written.isEmpty()) return;
        var tops = new Long2IntOpenHashMap();
        tops.defaultReturnValue(Integer.MIN_VALUE);
        for (long pos : written.keySet()) {
            long column = (BlockPos.getX(pos) & 15) | (BlockPos.getZ(pos) & 15) << 4;
            tops.put(column, Math.max(tops.get(column), BlockPos.getY(pos)));
        }
        int minY = level.getMinY();
        int bits = Mth.ceillog2(level.getHeight() + 1);
        for (var heightmap : heightmaps.entrySet()) {
            var storage = new SimpleBitStorage(bits, 256, heightmap.getValue());
            var opaque = heightmap.getKey().isOpaque();
            for (var iterator = tops.long2IntEntrySet().fastIterator(); iterator.hasNext(); ) {
                var entry = iterator.next();
                int index = (int) entry.getLongKey();
                int lx = index & 15, lz = index >> 4;
                int value = 0;
                for (int y = Math.max(storage.get(index) + minY - 1, entry.getIntValue());
                        y >= minY;
                        y--) {
                    if (opaque.test(
                            sections[level.getSectionIndex(y)].getBlockState(lx, y & 15, lz))) {
                        value = y + 1 - minY;
                        break;
                    }
                }
                storage.set(index, value);
            }
        }
    }

    private static Runtime startRuntime(ServerLevel level, Journal journal) {
        Runtime runtime = new Runtime(level, journal);
        RUNTIMES.put(level, runtime);
        WRITES.put(level.getChunkSource().chunkMap.storageInfo(), runtime);
        return runtime;
    }

    private static final class Op {
        private static final Codec<Holder<Biome>> BIOME = Biome.CODEC;
        private static final Codec<Op> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.LONG.fieldOf("id").forGetter(o -> o.id),
                                                Codec.DOUBLE.fieldOf("x").forGetter(o -> o.x),
                                                Codec.DOUBLE.fieldOf("z").forGetter(o -> o.z),
                                                Codec.INT.fieldOf("scale").forGetter(o -> o.scale),
                                                BuiltInRegistries.BLOCK
                                                        .byNameCodec()
                                                        .fieldOf("layer")
                                                        .forGetter(o -> o.layer),
                                                BIOME.optionalFieldOf("inner")
                                                        .forGetter(
                                                                o -> Optional.ofNullable(o.inner)),
                                                BIOME.optionalFieldOf("crater")
                                                        .forGetter(
                                                                o -> Optional.ofNullable(o.crater)),
                                                BIOME.optionalFieldOf("outer")
                                                        .forGetter(
                                                                o -> Optional.ofNullable(o.outer)),
                                                Codec.LONG.fieldOf("seed").forGetter(o -> o.seed),
                                                Codec.LONG_STREAM
                                                        .fieldOf("pending")
                                                        .forGetter(o -> LongStream.of(o.pending)))
                                        .apply(
                                                i,
                                                (id,
                                                        x,
                                                        z,
                                                        scale,
                                                        layer,
                                                        inner,
                                                        crater,
                                                        outer,
                                                        seed,
                                                        pending) ->
                                                        new Op(
                                                                id,
                                                                x,
                                                                z,
                                                                scale,
                                                                layer,
                                                                inner.orElse(null),
                                                                crater.orElse(null),
                                                                outer.orElse(null),
                                                                seed,
                                                                pending.toArray())));
        final long id;
        final double x;
        final double z;
        final int scale;
        final Block layer;
        final @Nullable Holder<Biome> inner;
        final @Nullable Holder<Biome> crater;
        final @Nullable Holder<Biome> outer;
        final long seed;
        final int minChunkX;
        final int minChunkZ;
        final int width;
        final long[] pending;
        int remaining;
        boolean published;
        final LongOpenHashSet resident = new LongOpenHashSet();

        private Op(
                long id,
                double x,
                double z,
                int scale,
                Block layer,
                @Nullable Holder<Biome> inner,
                @Nullable Holder<Biome> crater,
                @Nullable Holder<Biome> outer,
                long seed,
                long[] pending) {
            this.id = id;
            this.x = x;
            this.z = z;
            this.scale = scale;
            this.layer = layer;
            this.inner = inner;
            this.crater = crater;
            this.outer = outer;
            this.seed = seed;
            minChunkX = Mth.floor(x - scale) >> 4;
            minChunkZ = Mth.floor(z - scale) >> 4;
            width = (Mth.floor(x + scale) >> 4) - minChunkX + 1;
            int depth = (Mth.floor(z + scale) >> 4) - minChunkZ + 1;
            if (pending.length != (width * depth + 63) >> 6)
                throw new IllegalStateException("Fallout journal footprint mismatch");
            this.pending = pending;
            for (long word : pending) remaining += Long.bitCount(word);
        }

        static Op create(
                long id,
                double x,
                double z,
                int scale,
                Block layer,
                @Nullable Holder<Biome> inner,
                @Nullable Holder<Biome> crater,
                @Nullable Holder<Biome> outer,
                long seed) {
            int minChunkX = Mth.floor(x - scale) >> 4;
            int minChunkZ = Mth.floor(z - scale) >> 4;
            int width = (Mth.floor(x + scale) >> 4) - minChunkX + 1;
            int depth = (Mth.floor(z + scale) >> 4) - minChunkZ + 1;
            long[] pending = new long[(width * depth + 63) >> 6];
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    if (FalloutTerrain.ring(minChunkX + dx, minChunkZ + dz, x, z, scale)
                            != FalloutTerrain.NONE) {
                        int bit = dz * width + dx;
                        pending[bit >> 6] |= 1L << bit;
                    }
                }
            }
            return new Op(id, x, z, scale, layer, inner, crater, outer, seed, pending);
        }

        int ring(int chunkX, int chunkZ) {
            return FalloutTerrain.ring(chunkX, chunkZ, x, z, scale);
        }

        private int bit(int chunkX, int chunkZ) {
            int dx = chunkX - minChunkX, dz = chunkZ - minChunkZ;
            return dx < 0 || dz < 0 || dx >= width || dz * width + dx >= pending.length << 6
                    ? -1
                    : dz * width + dx;
        }

        boolean pendingAt(int chunkX, int chunkZ) {
            int bit = bit(chunkX, chunkZ);
            return bit >= 0 && ((long) WORDS.getOpaque(pending, bit >> 6) & 1L << bit) != 0;
        }

        boolean acknowledge(int chunkX, int chunkZ) {
            int bit = bit(chunkX, chunkZ);
            if (bit < 0) return false;
            long word = pending[bit >> 6];
            if ((word & 1L << bit) == 0) return false;
            WORDS.setOpaque(pending, bit >> 6, word & ~(1L << bit));
            remaining--;
            return true;
        }
    }

    private static final class Journal extends SavedData {
        private static final Codec<Journal> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.intRange(1, 1)
                                                        .fieldOf("version")
                                                        .forGetter(_ -> 1),
                                                Codec.LONG
                                                        .fieldOf("next_id")
                                                        .forGetter(j -> j.nextId),
                                                Op.CODEC
                                                        .listOf()
                                                        .fieldOf("operations")
                                                        .forGetter(
                                                                j ->
                                                                        new ArrayList<>(
                                                                                j.operations
                                                                                        .values())))
                                        .apply(
                                                i,
                                                (_, nextId, operations) ->
                                                        new Journal(nextId, operations)));
        private long nextId = 1;
        private final TreeMap<Long, Op> operations = new TreeMap<>();

        private Journal() {}

        private Journal(long nextId, List<Op> operations) {
            this.nextId = nextId;
            for (Op op : operations) this.operations.put(op.id, op);
        }
    }

    private static final class Runtime implements BombForkJoinPool.IJobCancellable {
        private final ServerLevel level;
        private final Journal journal;
        private volatile Op[] published = NONE;
        private final LongOpenHashSet evaluatedCores = new LongOpenHashSet();
        private @Nullable ForkJoinPool pool;
        private @Nullable BulkSectionUpdates updates;
        private final LongLinkedOpenHashSet ready = new LongLinkedOpenHashSet();
        private final LongOpenHashSet active = new LongOpenHashSet();
        private int countdown;
        private boolean closed;

        private Runtime(ServerLevel level, Journal journal) {
            this.level = level;
            this.journal = journal;
        }

        private void publishAll() {
            for (Op op : journal.operations.values()) publish(op);
        }

        private void publish(Op op) {
            if (closed || journal.operations.get(op.id) != op) return;
            op.published = true;
            republish();
            for (int dz = 0; dz * op.width < op.pending.length << 6; dz++) {
                for (int dx = 0; dx < op.width; dx++) {
                    int chunkX = op.minChunkX + dx, chunkZ = op.minChunkZ + dz;
                    if (op.ring(chunkX, chunkZ) == FalloutTerrain.NONE) continue;
                    LevelChunk chunk = ChunkUtil.liveChunkNow(level, ChunkPos.pack(chunkX, chunkZ));
                    if (chunk != null) loaded(chunk);
                }
            }
        }

        private void republish() {
            published =
                    journal.operations.values().stream()
                            .filter(o -> o.published)
                            .toArray(Op[]::new);
        }

        private boolean pendingAt(int chunkX, int chunkZ) {
            for (Op op : published) if (op.pendingAt(chunkX, chunkZ)) return true;
            return false;
        }

        private void loaded(LevelChunk chunk) {
            if (closed) return;
            int chunkX = chunk.getPos().x(), chunkZ = chunk.getPos().z();
            long key = chunk.getPos().pack();
            long marker =
                    ((com.hbm.interfaces.injected.ChunkExtension) (chunk))
                            .hbm$falloutAppliedThrough();
            boolean queued = false;
            boolean unacknowledged = false;
            for (Op op : published) {
                if (op.ring(chunkX, chunkZ) == FalloutTerrain.NONE) continue;
                if (op.id > marker) {
                    op.resident.add(key);
                    queued = true;
                } else if (op.pendingAt(chunkX, chunkZ)) {
                    unacknowledged = true;
                }
            }
            if (queued) ready.add(key);
            if (unacknowledged) chunk.markUnsaved();
        }

        private void unloaded(long key) {
            if (ready.remove(key)) {
                for (Op op : published) op.resident.remove(key);
            }
        }

        private void tick() {
            if (closed || --countdown > 0) return;
            countdown = BombConfig.fDelay;
            long deadline = System.nanoTime() + BombConfig.mk5 * 1_000_000L;
            if (updates != null) updates.drain(deadline);
            int remaining = ready.size();
            while (remaining-- > 0
                    && (pool == null || active.size() < pool.getParallelism() * 2)
                    && System.nanoTime() < deadline) {
                long key = ready.removeFirstLong();
                if (active.contains(key)) {
                    ready.add(key);
                    continue;
                }
                LevelChunk chunk = ChunkUtil.liveChunkNow(level, key);
                if (chunk == null) {
                    for (Op op : published) op.resident.remove(key);
                    continue;
                }
                List<Op> ops =
                        applicable(
                                published,
                                chunk.getPos().x(),
                                chunk.getPos().z(),
                                ((com.hbm.interfaces.injected.ChunkExtension) (chunk))
                                        .hbm$falloutAppliedThrough());
                if (ops.isEmpty()) {
                    for (Op op : published) op.resident.remove(key);
                    continue;
                }
                if (pool == null) {
                    pool = BombForkJoinPool.acquire();
                    updates =
                            new BulkSectionUpdates(
                                    level,
                                    pool,
                                    failure -> {
                                        throw new IllegalStateException(
                                                "Fallout journal catch-up failed", failure);
                                    });
                    BombForkJoinPool.register(pool, level.dimension().identifier(), this);
                }
                active.add(key);
                FalloutReplay replay =
                        ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$falloutReplay();
                updates.submit(
                        new Work(
                                key,
                                ops,
                                replay == null
                                        ? new Long2ObjectOpenHashMap<>()
                                        : new Long2ObjectOpenHashMap<>(replay.deferred)));
            }
            if (updates != null) {
                updates.drain(deadline);
                if (!updates.hasPending()) releaseWorkers();
            }
        }

        private void releaseWorkers() {
            updates.close();
            updates = null;
            BombForkJoinPool.unregister(pool, level.dimension().identifier(), this);
            BombForkJoinPool.release(pool);
            pool = null;
        }

        private void acknowledge(int chunkX, int chunkZ, long marker) {
            if (closed) return;
            boolean retired = false;
            boolean changed = false;
            for (var iterator = journal.operations.values().iterator(); iterator.hasNext(); ) {
                Op op = iterator.next();
                if (op.id > marker) break;
                if (!op.acknowledge(chunkX, chunkZ)) continue;
                changed = true;
                if (op.remaining == 0) {
                    iterator.remove();
                    retired = true;
                }
            }
            if (!changed) return;
            journal.setDirty();
            if (retired) republish();
            if (journal.operations.isEmpty()) {
                cancelJob();
                RUNTIMES.remove(level, this);
                WRITES.remove(level.getChunkSource().chunkMap.storageInfo(), this);
            }
        }

        @Override
        public void cancelJob() {
            if (closed) return;
            closed = true;
            if (updates != null) releaseWorkers();
            ready.clear();
            active.clear();
        }

        private final class Work extends BulkSectionUpdates.Work {
            private final long key;
            private final List<Op> ops;
            private final List<FalloutEntry> table =
                    FalloutConfigJSON.entries(level.registryAccess());
            private final Long2ObjectOpenHashMap<BlockState> deferred;
            private final Long2ObjectOpenHashMap<BlockState> written =
                    new Long2ObjectOpenHashMap<>();
            private final Long2ObjectOpenHashMap<BlockState> fallers =
                    new Long2ObjectOpenHashMap<>();
            private final LongOpenHashSet surfaces = new LongOpenHashSet();
            private final Long2ObjectOpenHashMap<BlockState> initialDeferred;
            private boolean biomes;

            private Work(long key, List<Op> ops, Long2ObjectOpenHashMap<BlockState> deferred) {
                super(key);
                this.key = key;
                this.ops = ops;
                this.deferred = deferred;
                initialDeferred = new Long2ObjectOpenHashMap<>(deferred);
            }

            @Override
            protected long observedSections() {
                return -1L;
            }

            @Override
            protected void calculate(SectionSnapshot snapshot) {
                int chunkX = ChunkPos.getX(key), chunkZ = ChunkPos.getZ(key);
                FalloutRainEntity.WorkerScratch scratch = SCRATCH.get();
                for (Op op : ops) {
                    scratch.clear();
                    FalloutTerrain.convert(
                            level,
                            snapshot.sections,
                            deferred,
                            scratch,
                            chunkX,
                            chunkZ,
                            op.x,
                            op.z,
                            op.scale,
                            op.ring(chunkX, chunkZ) == FalloutTerrain.OUTER,
                            op.layer,
                            table,
                            FalloutTerrain.chunkSeed(op.seed, key));
                    FalloutTerrain.dropUnsupportedLayers(
                            level, snapshot.sections, deferred, scratch.updates, op.layer);
                    ChunkUtil.applyToSections(
                            snapshot.sections,
                            level.getMinSectionY(),
                            chunkX,
                            chunkZ,
                            scratch.updates,
                            written,
                            deferred,
                            true,
                            snapshot::changed);
                    for (var iterator = scratch.spawnFalling.long2ObjectEntrySet().fastIterator();
                            iterator.hasNext(); ) {
                        var entry = iterator.next();
                        fallers.putIfAbsent(entry.getLongKey(), entry.getValue());
                    }
                    surfaces.addAll(scratch.fallingSurfaceCells);
                    if (op.inner != null) {
                        FalloutTerrain.repaintBiomes(
                                snapshot.sections,
                                chunkX,
                                chunkZ,
                                op.x,
                                op.z,
                                op.scale,
                                op.inner,
                                op.crater,
                                op.outer,
                                index -> {
                                    snapshot.changed(index);
                                    biomes = true;
                                });
                    }
                }
            }

            @Override
            protected void published(LevelChunk chunk) {
                active.remove(key);
                for (Op op : ops) op.resident.remove(key);
                if (applicable(published, chunk.getPos().x(), chunk.getPos().z(), ops.getLast().id)
                        .isEmpty()) {
                    ready.remove(key);
                }
                ((com.hbm.interfaces.injected.ChunkExtension) (chunk))
                        .hbm$setFalloutAppliedThrough(ops.getLast().id);
                var lightChanges = new LongArrayList();
                long sectionMask = 0;
                int minSectionY = chunk.getMinSectionY();
                var pos = new BlockPos.MutableBlockPos();
                for (var iterator = written.long2ObjectEntrySet().fastIterator();
                        iterator.hasNext(); ) {
                    var entry = iterator.next();
                    pos.set(entry.getLongKey());
                    BlockState old = entry.getValue();
                    BlockState now = chunk.getBlockState(pos);
                    int lx = pos.getX() & 15, y = pos.getY(), lz = pos.getZ() & 15;
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING)
                            .update(lx, y, lz, now);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)
                            .update(lx, y, lz, now);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR)
                            .update(lx, y, lz, now);
                    chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE)
                            .update(lx, y, lz, now);
                    if (LightEngine.hasDifferentLightProperties(old, now))
                        lightChanges.add(entry.getLongKey());
                    level.updatePOIOnBlockStateChange(pos, old, now);
                    sectionMask |= 1L << ((y >> 4) - minSectionY);
                }
                FalloutReplay replay =
                        ((com.hbm.interfaces.injected.ChunkExtension) (chunk)).hbm$falloutReplay();
                if (replay == null) replay = new FalloutReplay(level.getMinY());
                for (var iterator = written.long2ObjectEntrySet().fastIterator();
                        iterator.hasNext(); ) {
                    var entry = iterator.next();
                    replay.changed.putIfAbsent(entry.getLongKey(), entry.getValue());
                }
                for (long cell : initialDeferred.keySet()) replay.deferred.remove(cell);
                replay.deferred.putAll(deferred);
                for (long cell : surfaces) {
                    int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
                    long core =
                            MultiblockSurface.foldedCore(chunk.getBlockState(pos.set(cell))) != null
                                    ? cell
                                    : MultiblockSurface.indexedCorePacked(level, x, y, z);
                    if (MultiblockSurface.hasCore(core)) replay.cores.add(core);
                }
                admitFallers(chunk);
                if (replay.persistable())
                    ((com.hbm.interfaces.injected.ChunkExtension) (chunk))
                            .hbm$setFalloutReplay(replay);
                if (biomes) level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
                CompletableFuture<?> lighting =
                        ChunkUtil.updateLight(level, chunk, lightChanges, sectionMask);
                if (chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))
                    replay(level, chunk);
                ChunkUtil.broadcastWholesaleResend(level, key, lighting);
                chunk.markUnsaved();
            }

            private void admitFallers(LevelChunk chunk) {
                if (fallers.isEmpty()) return;
                var origin =
                        new BlockPos(
                                chunk.getPos().getMinBlockX(),
                                level.getMinY(),
                                chunk.getPos().getMinBlockZ());
                if (PendingFallers.canDefer(level, origin)) {
                    PendingFallers.appendAll(chunk, fallers);
                    return;
                }
                FalloutRainEntity.spawnFallers(level, chunk, fallers);
            }

            @Override
            protected void missing() {
                active.remove(key);
                ready.remove(key);
                for (Op op : ops) op.resident.remove(key);
            }

            @Override
            protected void clearAttempt() {
                written.clear();
                fallers.clear();
                surfaces.clear();
                deferred.clear();
                deferred.putAll(initialDeferred);
                biomes = false;
            }

            @Override
            protected void discard() {
                clearAttempt();
            }
        }
    }
}
