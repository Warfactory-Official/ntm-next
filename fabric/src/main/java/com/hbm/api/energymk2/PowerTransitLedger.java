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

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PowerTransitLedger extends SavedData {
    private static final Codec<Entry> ENTRY =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG.fieldOf("anchor").forGetter(Entry::anchor),
                                            Codec.LONG
                                                    .fieldOf("injected")
                                                    .forGetter(Entry::injected),
                                            Codec.LONG.fieldOf("owed").forGetter(Entry::owed),
                                            Codec.LONG.fieldOf("he_to_fe").forGetter(Entry::heToFe),
                                            Codec.LONG.fieldOf("fe_to_he").forGetter(Entry::feToHe))
                                    .apply(i, Entry::new));
    static final Codec<PowerTransitLedger> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ENTRY.listOf()
                                                    .fieldOf("pools")
                                                    .forGetter(PowerTransitLedger::entries))
                                    .apply(i, PowerTransitLedger::new));
    public static final SavedDataType<PowerTransitLedger> TYPE =
            new SavedDataType<>(
                    RegistryUtil.id("power_transit"), PowerTransitLedger::new, CODEC, null);
    private final Long2ObjectOpenHashMap<Entry> pending = new Long2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<ForeignEnergyBridge, NodeNetwork<CableData>> live =
            new Reference2ObjectOpenHashMap<>();
    private boolean restored;

    private PowerTransitLedger() {}

    private PowerTransitLedger(List<Entry> entries) {
        for (Entry entry : entries)
            if (pending.put(entry.anchor, entry) != null)
                throw new IllegalArgumentException(
                        "Duplicate power transit anchor: " + entry.anchor);
    }

    static PowerTransitLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    void restore(ServerLevel level, LevelNodeGraph<CableData> graph) {
        if (restored) return;
        restored = true;
        while (!pending.isEmpty()) {
            long anchor = pending.keySet().iterator().nextLong();
            NodeNetwork<CableData> net = graph.networkAt(anchor);
            if (net == null) {
                pending.remove(anchor);
                setDirty();
            } else PowerNetwork.restoreBridge(level, graph, net);
        }
    }

    void bind(
            LevelNodeGraph<CableData> graph,
            NodeNetwork<CableData> net,
            ForeignEnergyBridge bridge) {
        live.put(bridge, net);
        bridge.ledger = this;
        var iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (graph.networkAt(entry.anchor) != net) continue;
            bridge.restoreConserved(entry.injected, entry.owed, entry.heToFe, entry.feToHe);
            iterator.remove();
            setDirty();
        }
    }

    void move(ForeignEnergyBridge bridge, NodeNetwork<CableData> net) {
        live.put(bridge, net);
        setDirty();
    }

    void retire(ForeignEnergyBridge bridge) {
        live.remove(bridge);
        setDirty();
    }

    private List<Entry> entries() {
        var entries = new ArrayList<>(pending.values());
        var iterator = live.reference2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            NodeNetwork<CableData> net = entry.getValue();
            ForeignEnergyBridge bridge = entry.getKey();
            if (net.nodes.isEmpty()) {
                iterator.remove();
                continue;
            }
            ForeignEnergyBridge.State state = bridge.snapshot();
            if (state.injected() == 0
                    && state.owed() == 0
                    && state.heToFeCarry() == 0
                    && state.feToHeCarry() == 0) continue;
            entries.add(
                    new Entry(
                            net.nodes.iterator().next().posKey,
                            state.injected(),
                            state.owed(),
                            state.heToFeCarry(),
                            state.feToHeCarry()));
        }
        return entries;
    }

    private record Entry(long anchor, long injected, long owed, long heToFe, long feToHe) {
        Entry {
            if (injected < 0 || owed < 0 || heToFe < 0 || feToHe < 0)
                throw new IllegalArgumentException("Negative conserved power at " + anchor);
        }
    }
}
