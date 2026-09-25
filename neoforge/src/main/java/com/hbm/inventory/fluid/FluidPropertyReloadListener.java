// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.google.gson.JsonElement;
import com.hbm.lib.Library;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FluidPropertyReloadListener
        extends SimpleJsonResourceReloadListener<JsonElement> {

    public static final Identifier ID = Library.id("fluid_property");

    public FluidPropertyReloadListener() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("fluid_property"));
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> entries,
            ResourceManager manager,
            ProfilerFiller profiler) {
        List<String> failures = new ArrayList<>();
        int loaded = 0;

        Map<Fluid, NTMFluidProperty> replacement = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : entries.entrySet()) {
            Identifier id = entry.getKey();
            Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
            if (fluid == Fluids.EMPTY) {
                failures.add(id + " names no registered fluid");
                continue;
            }
            DataResult<NTMFluidProperty> parsed =
                    NTMFluidPropertyCodec.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
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
