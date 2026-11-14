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

package com.hbm.render.loader;

import com.hbm.util.RegistryUtil;
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

    public static final Identifier ID = RegistryUtil.id("obj_meshes");
    public static final ObjMeshCache INSTANCE = new ObjMeshCache();
    private static volatile IdentityHashMap<
                    ResourceManager, ConcurrentHashMap<Identifier, GroupObject[]>>
            caches = new IdentityHashMap<>();

    private ObjMeshCache() {}

    static GroupObject[] load(ResourceManager resources, Identifier id) {
        var generation = caches;
        ConcurrentHashMap<Identifier, GroupObject[]> cache;
        synchronized (generation) {
            cache =
                    generation.computeIfAbsent(
                            resources, _ -> new ConcurrentHashMap<>());
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
