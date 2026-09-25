// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import com.hbm.lib.Library;
import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ObjMeshCache implements PreparableReloadListener {

    public static final Identifier ID = Library.id("obj_meshes");
    public static final ObjMeshCache INSTANCE = new ObjMeshCache();
    private static volatile IdentityHashMap<
                    ResourceManager, ConcurrentHashMap<Identifier, GroupObject[]>>
            caches = new IdentityHashMap<>();

    private ObjMeshCache() {}

    static GroupObject[] load(ResourceManager resources, Identifier id) {
        var generation = caches;
        ConcurrentHashMap<Identifier, GroupObject[]> cache;
        synchronized (generation) {
            cache = generation.computeIfAbsent(resources, _ -> new ConcurrentHashMap<>());
        }
        GroupObject[] shared =
                cache.computeIfAbsent(
                        id,
                        obj -> {
                            Identifier compiled = ObjMesh.resource(obj);
                            try (InputStream input =
                                    resources.getResourceOrThrow(compiled).open()) {
                                return ObjMesh.read(input, compiled.toString());
                            } catch (IOException e) {
                                throw new IllegalStateException(
                                        "Failed to load mesh " + compiled, e);
                            }
                        });
        GroupObject[] groups = new GroupObject[shared.length];
        for (int i = 0; i < groups.length; i++) groups[i] = shared[i].copy();
        return groups;
    }

    @Override
    public void prepareSharedState(SharedState currentReload) {

        caches = new IdentityHashMap<>();
    }

    @Override
    public CompletableFuture<Void> reload(
            SharedState currentReload,
            Executor taskExecutor,
            PreparationBarrier preparationBarrier,
            Executor reloadExecutor) {
        return preparationBarrier.wait(null);
    }
}
