// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.NuclearTech;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

final class RegionChunkRadiationStorage implements ChunkRadiationStorage {
    private static final String PAYLOAD_KEY = "payload";
    private static final String STORAGE_TYPE = "hbm_radiation";

    private final ConcurrentHashMap<Path, RadiationRegionStorage> storages =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, Object> rootLocks = new ConcurrentHashMap<>();

    private static RadiationRegionStorage.Backend backend(ServerLevel level, Path root) {
        RegionFileStorage storage =
                new RegionFileStorage(
                        new RegionStorageInfo(
                                level.dimension().identifier().toString(),
                                level.dimension(),
                                STORAGE_TYPE),
                        root,
                        level.getServer().forceSynchronousWrites());
        return new RadiationRegionStorage.Backend() {
            @Override
            public boolean hasRegion(ChunkPos pos) throws IOException {
                Path region = regionPath(root, pos);
                if (!Files.exists(region)) return false;
                if (Files.size(region) != 0L) return true;
                Files.deleteIfExists(region);
                return false;
            }

            @Override
            public byte @Nullable [] read(ChunkPos pos) throws IOException {
                CompoundTag tag = storage.read(pos);
                return tag == null ? null : tag.getByteArray(PAYLOAD_KEY).orElse(null);
            }

            @Override
            public void write(ChunkPos pos, byte @Nullable [] payload) throws IOException {
                if (payload == null || payload.length == 0) {
                    storage.write(pos, null);
                } else {
                    CompoundTag tag = new CompoundTag();
                    tag.putByteArray(PAYLOAD_KEY, payload);
                    storage.write(pos, tag);
                }
            }

            @Override
            public void flush() throws IOException {
                storage.flush();
            }

            @Override
            public void close() throws IOException {
                storage.close();
            }
        };
    }

    private static Path dimensionRoot(ServerLevel level) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        return DimensionType.getStorageFolder(level.dimension(), root)
                .resolve("hbm")
                .resolve("radiation");
    }

    private static Path regionPath(Path root, ChunkPos pos) {
        return root.resolve(
                "r."
                        + pos.getRegionX()
                        + "."
                        + pos.getRegionZ()
                        + RegionFileStorage.ANVIL_EXTENSION);
    }

    static Path retireDirectory(Path root) throws IOException {
        Path trash = nextTrashPath(root);
        Files.move(root, trash, StandardCopyOption.ATOMIC_MOVE);
        return trash;
    }

    private static Path nextTrashPath(Path root) {
        Path parent = root.getParent();
        String base =
                root.getFileName() + ".trash." + Long.toUnsignedString(System.currentTimeMillis());
        Path trash = parent.resolve(base);
        int suffix = 0;
        while (Files.exists(trash)) trash = parent.resolve(base + "." + ++suffix);
        return trash;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            Iterator<Path> it = paths.sorted(Comparator.reverseOrder()).iterator();
            while (it.hasNext()) {
                Files.deleteIfExists(it.next());
            }
        }
    }

    @Override
    public byte @Nullable [] read(ServerLevel level, ChunkPos pos) {
        Path root = dimensionRoot(level);
        synchronized (rootLock(root)) {
            RadiationRegionStorage open = storages.get(root);
            if (open == null) {
                Path region = regionPath(root, pos);
                if (!Files.exists(region)) return null;
                try {
                    if (Files.size(region) == 0L) {
                        Files.deleteIfExists(region);
                        return null;
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            return (open != null ? open : storage(level, root)).read(pos);
        }
    }

    @Override
    public void write(ServerLevel level, ChunkPos pos, byte @Nullable [] payload) {
        Path root = dimensionRoot(level);
        synchronized (rootLock(root)) {
            RadiationRegionStorage open = storages.get(root);
            if (payload == null && open == null) {
                Path region = regionPath(root, pos);
                if (!Files.exists(region)) return;
                try {
                    if (Files.size(region) == 0L) {
                        Files.deleteIfExists(region);
                        return;
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            (open != null ? open : storage(level, root)).write(pos, payload);
        }
    }

    @Override
    public void discard(ServerLevel level, ChunkPos pos) {
        Path root = dimensionRoot(level);
        synchronized (rootLock(root)) {
            RadiationRegionStorage open = storages.get(root);
            if (open == null) {
                Path region = regionPath(root, pos);
                if (!Files.exists(region)) return;
                try {
                    if (Files.size(region) == 0L) {
                        Files.deleteIfExists(region);
                        return;
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            (open != null ? open : storage(level, root)).write(pos, null);
        }
    }

    @Override
    public void flush() {
        RuntimeException failure = null;
        for (RadiationRegionStorage storage : storages.values()) {
            try {
                storage.flush();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    @Override
    public void deleteDimension(ServerLevel level) {
        Path root = dimensionRoot(level);
        Path trash;
        synchronized (rootLock(root)) {
            RadiationRegionStorage storage = storages.get(root);
            if (storage != null) {
                storage.flush();
                try {
                    storage.closeAfterFlush();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            storages.remove(root, storage);
            if (!Files.exists(root)) return;
            try {
                trash = retireDirectory(root);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        Util.ioPool()
                .execute(
                        () -> {
                            try {
                                deleteRecursively(trash);
                            } catch (IOException ex) {
                                NuclearTech.LOGGER.warn(
                                        "Failed to delete retired radiation sidecar directory {}",
                                        trash,
                                        ex);
                            }
                        });
    }

    @Override
    public void close() {
        RuntimeException failure = null;
        for (RadiationRegionStorage storage : storages.values()) {
            try {
                storage.close();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        storages.clear();
        rootLocks.clear();
        if (failure != null) throw failure;
    }

    private RadiationRegionStorage storage(ServerLevel level, Path root) {
        return storages.computeIfAbsent(root, r -> new RadiationRegionStorage(level, r));
    }

    private Object rootLock(Path root) {
        return rootLocks.computeIfAbsent(root, ignored -> new Object());
    }

    static final class RadiationRegionStorage {
        private final Backend backend;
        private final Long2ObjectOpenHashMap<PendingWrite> pendingWrites =
                new Long2ObjectOpenHashMap<>();
        private boolean needsFlush;

        RadiationRegionStorage(ServerLevel level, Path root) {
            this(backend(level, root));
        }

        RadiationRegionStorage(Backend backend) {
            this.backend = backend;
        }

        synchronized byte @Nullable [] read(ChunkPos pos) {
            PendingWrite pending = pendingWrites.get(pos.pack());
            if (pending != null) return pending.payload;
            try {
                if (!backend.hasRegion(pos)) return null;
                return backend.read(pos);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        synchronized void write(ChunkPos pos, byte @Nullable [] payload) {
            PendingWrite pending = stage(pos, payload);
            writePending(pending);
        }

        synchronized PendingWrite stage(ChunkPos pos, byte @Nullable [] payload) {
            PendingWrite pending = new PendingWrite(pos, payload);
            pendingWrites.put(pos.pack(), pending);
            return pending;
        }

        private void writePending(PendingWrite pending) {
            try {
                if (pending.payload == null || pending.payload.length == 0) {
                    if (!backend.hasRegion(pending.pos)) {
                        acknowledge(pending);
                        return;
                    }
                    backend.write(pending.pos, null);
                } else {
                    backend.write(pending.pos, pending.payload);
                }
                needsFlush = true;
                acknowledge(pending);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private void acknowledge(PendingWrite pending) {
            long key = pending.pos.pack();
            if (pendingWrites.get(key) == pending) pendingWrites.remove(key);
        }

        synchronized void flush() {
            PendingWrite[] pending = pendingWrites.values().toArray(PendingWrite[]::new);
            RuntimeException failure = null;
            for (PendingWrite write : pending) {
                try {
                    writePending(write);
                } catch (RuntimeException ex) {
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            if (failure != null) throw failure;
            if (!needsFlush) return;
            try {
                backend.flush();
                needsFlush = false;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        synchronized void close() {
            RuntimeException failure = null;
            try {
                flush();
            } catch (RuntimeException ex) {
                failure = ex;
            }
            try {
                closeAfterFlush();
            } catch (IOException ex) {
                RuntimeException closeFailure = new UncheckedIOException(ex);
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
            if (failure != null) throw failure;
        }

        synchronized void closeAfterFlush() throws IOException {
            backend.close();
        }

        interface Backend {
            boolean hasRegion(ChunkPos pos) throws IOException;

            byte @Nullable [] read(ChunkPos pos) throws IOException;

            void write(ChunkPos pos, byte @Nullable [] payload) throws IOException;

            void flush() throws IOException;

            void close() throws IOException;
        }

        private record PendingWrite(ChunkPos pos, byte @Nullable [] payload) {}
    }
}
