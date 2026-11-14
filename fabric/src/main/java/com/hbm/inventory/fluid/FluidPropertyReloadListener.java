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

package com.hbm.inventory.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.registration.Shared;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FluidPropertyReloadListener
        extends SimplePreparableReloadListener<FluidPropertyReloadListener.Files> {

    public static final Identifier ID = RegistryUtil.id("fluid_property");

    private final JsonDirectory properties = new JsonDirectory("fluid_property");
    private final JsonDirectory patches = new JsonDirectory("fluid_property_patch");

    private static final class JsonDirectory extends SimpleJsonResourceReloadListener<JsonElement> {
        private JsonDirectory(String path) {
            super(ExtraCodecs.JSON, FileToIdConverter.json(path));
        }

        @Override
        public Map<Identifier, JsonElement> prepare(
                ResourceManager manager, ProfilerFiller profiler) {
            return super.prepare(manager, profiler);
        }

        @Override
        protected void apply(
                Map<Identifier, JsonElement> files,
                ResourceManager manager,
                ProfilerFiller profiler) {
            throw new UnsupportedOperationException();
        }
    }

    public record Files(
            Map<Identifier, JsonElement> properties, Map<Identifier, JsonElement> patches) {}

    @Override
    protected Files prepare(ResourceManager manager, ProfilerFiller profiler) {
        return new Files(properties.prepare(manager, profiler), patches.prepare(manager, profiler));
    }

    @Override
    protected void apply(Files files, ResourceManager manager, ProfilerFiller profiler) {
        List<String> failures = new ArrayList<>();
        int loaded = 0;

        Map<Identifier, Map<Identifier, JsonObject>> patches = new HashMap<>();
        for (Map.Entry<Identifier, JsonElement> patch : files.patches().entrySet()) {
            String path = patch.getKey().getPath();
            int slash = path.indexOf('/');
            Identifier target =
                    slash < 0
                            ? null
                            : Identifier.tryBuild(
                                    path.substring(0, slash), path.substring(slash + 1));
            if (target == null || !files.properties().containsKey(target)) {
                failures.add(patch.getKey() + " patches no fluid property");
                continue;
            }
            patches.computeIfAbsent(target, id -> new TreeMap<>())
                    .put(patch.getKey(), patch.getValue().getAsJsonObject());
        }

        Map<Fluid, NTMFluidProperty> replacement = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : files.properties().entrySet()) {
            Identifier id = entry.getKey();
            if (Shared.isOtherCopy(Registries.FLUID, id)) continue;
            Identifier shared = Shared.registryId(Registries.FLUID, id);
            Fluid fluid = BuiltInRegistries.FLUID.getValue(shared != null ? shared : id);
            if (fluid == Fluids.EMPTY) {
                failures.add(id + " names no registered fluid");
                continue;
            }
            JsonElement json = entry.getValue();
            Map<Identifier, JsonObject> edits = patches.get(id);
            if (edits != null) {
                try {
                    json = FluidPropertyPatch.apply(id, json.getAsJsonObject(), edits);
                } catch (IllegalStateException conflict) {
                    failures.add(conflict.getMessage());
                    continue;
                }
            }
            DataResult<NTMFluidProperty> parsed =
                    NTMFluidPropertyCodec.CODEC.parse(JsonOps.INSTANCE, json);
            if (parsed.error().isPresent()) {
                failures.add(id + ": " + parsed.error().get().message());
                continue;
            }
            replacement.put(fluid, parsed.result().orElseThrow());
            loaded++;
        }

        if (!failures.isEmpty()) {
            throw new IllegalStateException(
                    failures.size()
                            + " fluid property file(s) failed to load: "
                            + failures.subList(0, Math.min(5, failures.size())));
        }
        if (loaded == 0) {
            throw new IllegalStateException(
                    "no fluid property loaded - every machine, gauge and tooltip "
                            + "would read as though no fluid had properties at all");
        }
        NTMFluidProperties.replace(replacement);
        NTMFluidProperties.markResyncPending();
    }
}
