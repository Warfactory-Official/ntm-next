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

package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.trait.FluidTraitSimple.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class FluidTraitRegistry {

    private record Entry(String name, Class<? extends FluidTrait> kind) {}

    private record Snapshot(
            List<Entry> entries,
            Map<String, Class<? extends FluidTrait>> byName,
            Map<Class<? extends FluidTrait>, String> byKind,
            List<Class<? extends FluidTrait>> kinds) {

        static Snapshot of(List<Entry> entries) {
            var byName = new LinkedHashMap<String, Class<? extends FluidTrait>>();
            var byKind = new HashMap<Class<? extends FluidTrait>, String>();
            var kinds = new ArrayList<Class<? extends FluidTrait>>();
            for (var entry : entries) {
                if (byName.putIfAbsent(entry.name(), entry.kind()) != null) {
                    throw new IllegalStateException(
                            "trait name '" + entry.name() + "' is already registered");
                }
                if (byKind.putIfAbsent(entry.kind(), entry.name()) != null) {
                    throw new IllegalStateException(
                            entry.kind().getName()
                                    + " is already registered as '"
                                    + byKind.get(entry.kind())
                                    + "'");
                }
                kinds.add(entry.kind());
            }
            return new Snapshot(
                    List.copyOf(entries),
                    Collections.unmodifiableMap(byName),
                    Map.copyOf(byKind),
                    List.copyOf(kinds));
        }
    }

    private static volatile Snapshot snapshot =
            Snapshot.of(
                    List.of(
                            new Entry("corrosive", CorrosiveTrait.class),
                            new Entry("flammable", FlammableTrait.class),
                            new Entry("combustible", CombustibleTrait.class),
                            new Entry("polluting", PollutingTrait.class),
                            new Entry("heatable", HeatableTrait.class),
                            new Entry("coolable", CoolableTrait.class),
                            new Entry("pwrmoderator", ReactorModeratorTrait.class),
                            new Entry("poison", PoisonTrait.class),
                            new Entry("toxin", ToxinTrait.class),
                            new Entry("ventradiation", RadiationReleaseTrait.class),
                            new Entry("pheromone", PheromoneTrait.class),
                            new Entry("gaseous", FT_Gaseous.class),
                            new Entry("gaseous_art", FT_Gaseous_ART.class),
                            new Entry("liquid", FT_Liquid.class),
                            new Entry("viscous", FT_Viscous.class),
                            new Entry("plasma", FT_Plasma.class),
                            new Entry("amat", FT_Amat.class),
                            new Entry("leadcontainer", FT_LeadContainer.class),
                            new Entry("delicious", FT_Delicious.class),
                            new Entry("noid", FT_NoID.class),
                            new Entry("nocontainer", FT_NoContainer.class),
                            new Entry("unsiphonable", FT_Unsiphonable.class),
                            new Entry("canister", CD_Canister.class),
                            new Entry("gastank", CD_Gastank.class)));

    private FluidTraitRegistry() {}

    public static synchronized void insertAfter(
            String anchor, String name, Class<? extends FluidTrait> kind) {
        var entries = new ArrayList<>(snapshot.entries());
        var at = 0;
        while (at < entries.size() && !entries.get(at).name().equals(anchor)) at++;
        if (at == entries.size()) {
            throw new IllegalStateException(
                    "anchor trait '" + anchor + "' for '" + name + "' is not registered");
        }
        entries.add(at + 1, new Entry(name, kind));
        snapshot = Snapshot.of(entries);
    }

    public static @Nullable Class<? extends FluidTrait> kind(String name) {
        return snapshot.byName().get(name);
    }

    public static @Nullable String nameOf(Class<? extends FluidTrait> kind) {
        return snapshot.byKind().get(kind);
    }

    public static Set<String> names() {
        return snapshot.byName().keySet();
    }

    public static List<Class<? extends FluidTrait>> kinds() {
        return snapshot.kinds();
    }
}
