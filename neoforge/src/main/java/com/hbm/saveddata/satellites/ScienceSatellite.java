/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.saveddata.satellites;

import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DriveType;
import com.hbm.util.NumberFormatter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class ScienceSatellite extends Satellite {

    public static final int COOLDOWN = 18_000;
    private static final int SENSOR_BUDGET = 7_200_000;
    public static final MapCodec<ScienceSatellite> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("lastScience", 0L)
                                                    .forGetter(sat -> sat.flightDataAt),
                                            Codec.INT
                                                    .optionalFieldOf("sensorProgress", 0)
                                                    .forGetter(sat -> sat.measured),
                                            Codec.INT
                                                    .optionalFieldOf("sensorCount", 0)
                                                    .forGetter(sat -> sat.sensors),
                                            Codec.INT
                                                    .optionalFieldOf("assemblerCount", 0)
                                                    .forGetter(sat -> sat.assemblers),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("assemblerProgress", 0D)
                                                    .forGetter(sat -> sat.assembly),
                                            Codec.STRING
                                                    .listOf()
                                                    .optionalFieldOf("assemblerTasks", List.of())
                                                    .forGetter(sat -> sat.tasks))
                                    .apply(i, ScienceSatellite::new));
    private static final Component ORBIT_DATA = Component.translatable("satellite.data");

    public long flightDataAt;
    public int measured;
    public int sensors;
    public int assemblers;
    public double assembly;
    public final List<String> tasks = new ArrayList<>();

    public ScienceSatellite() {}

    private ScienceSatellite(
            long flightDataAt,
            int measured,
            int sensors,
            int assemblers,
            double assembly,
            List<String> tasks) {
        this.flightDataAt = flightDataAt;
        this.measured = measured;
        this.sensors = sensors;
        this.assemblers = assemblers;
        this.assembly = assembly;
        this.tasks.addAll(tasks);
    }

    @Override
    public SatelliteType kind() {
        return SatelliteType.SCIENCE;
    }

    @Override
    public boolean dataReady(ServerLevel level) {
        if (offer() != null) return true;
        long now = level.getGameTime();
        if (now > flightDataAt + COOLDOWN) {
            offerData(DriveType.DISK_EMPTY, DriveType.DISK_FLIGHTDATA);
            flightDataAt = now;
            changed();
        }
        return offer() != null;
    }

    @Override
    public void receivePart(ServerLevel level, ItemStack part) {
        if (part.is(ModItems.SATELLITE_SCIENCE_SENSOR.get())) {
            sensors++;
        } else if (part.is(ModItems.SATELLITE_SCIENCE_ASSEMBLER.get())) {
            assemblers++;
        } else {
            var recipe = SpaceAssemblerRecipes.INSTANCE.getRecipeFor(part);
            if (recipe == null) return;
            tasks.add(recipe.getInternalName());
        }
        changed();
    }

    @Override
    public void tick(ServerLevel level) {
        if (measured >= SENSOR_BUDGET) {
            measured = 0;
            offerData(DriveType.DISK_EMPTY, DriveType.DISK_ORBITDATA);
            changed();
        } else if (sensors > 0) {
            measured += sensors;
            changed();
        }
        if (assemblers > 0 && cargo().isEmpty() && !tasks.isEmpty()) assemble(level);
    }

    private void assemble(ServerLevel level) {
        var recipe = SpaceAssemblerRecipes.INSTANCE.getRecipe(tasks.getFirst());
        if (recipe == null) {
            tasks.removeFirst();
            assembly = 0D;
            changed();
            return;
        }
        assembly += (double) assemblers / recipe.duration;
        changed();
        if (assembly < 1D) return;
        var cargo = cargo();
        var random = level.getRandom();
        for (var output : recipe.outputItems()) {
            cargo.add(output.getRandom(random).map(ItemStack::copy).orElse(ItemStack.EMPTY));
        }
        tasks.removeFirst();
        assembly = 0D;
    }

    @Override
    public List<Component> info(ServerLevel level) {
        var lines = new ArrayList<Component>(8);
        lines.add(kind().stationName());
        long left = flightDataAt + COOLDOWN - level.getGameTime();
        if (left <= 0) {
            lines.add(READY);
        } else {
            long seconds = left / 20;
            lines.add(
                    Component.translatable(
                            "satellite.cooldown",
                            Component.translatable(
                                    "unit.minutes_seconds", seconds / 60, seconds % 60)));
        }
        if (sensors > 0) {
            lines.add(Component.translatable("satellite.sensors", sensors));
            lines.add(
                    Component.translatable(
                            "satellite.pending",
                            NumberFormatter.formatGrouped((long) SENSOR_BUDGET - measured)));
        }
        var pending = offer();
        if (pending != null && pending.output() == DriveType.DISK_ORBITDATA) lines.add(ORBIT_DATA);
        if (assemblers > 0) {
            lines.add(Component.translatable("satellite.assemblers", assemblers));
            lines.add(
                    Component.translatable(
                            "satellite.progress",
                            Component.translatable("unit.percent", Math.round(assembly * 100D))));
            lines.add(Component.translatable("satellite.queue", tasks.size()));
        }
        return lines;
    }
}
