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

package com.hbm.saveddata.satellites;

import com.hbm.registration.OverridePoint;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class SatelliteDeployment {
    public static final OverridePoint<List<? extends Supplier<? extends DataComponentType<?>>>>
            CARRIED = OverridePoint.of(RegistryUtil.id("satellite_carried"), List.of());

    public static final OverridePoint<Destination> ORBIT_LEVEL =
            OverridePoint.of(
                    RegistryUtil.id("satellite_orbit_level"),
                    (level, _) -> level);

    private SatelliteDeployment() {}

    @FunctionalInterface
    public interface Destination {
        ServerLevel orbitOf(ServerLevel launch, SatelliteType kind);
    }

    public static boolean launch(
            ServerLevel level, ItemStack payload, int frequency, double x, double y, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        var registry = SatelliteSavedData.at(level, blockX, blockZ, frequency);
        var occupant = registry.getSatFromFreq(frequency);
        if (occupant != null) {
            occupant.receivePart(level, payload);
            registry.setDirty();
            return true;
        }

        var kind = SatelliteType.fromStack(payload);
        if (kind == null) return false;
        var orbit = ORBIT_LEVEL.get().orbitOf(level, kind);
        var home = SatelliteSavedData.at(orbit, blockX, blockZ);
        var satellite = kind.create();
        satellite.carry(carriedOf(payload));
        home.put(frequency, satellite);
        satellite.aim(blockX, blockZ);
        satellite.enterOrbit(orbit, x, y, z);
        home.setDirty();
        return true;
    }

    public static boolean copyCarried(ItemStack from, ItemStack to) {
        boolean changed = false;
        for (var type : CARRIED.get()) changed |= copy(type.get(), from, to);
        return changed;
    }

    private static <T> boolean copy(DataComponentType<T> type, ItemStack from, ItemStack to) {
        T value = from.get(type);
        if (Objects.equals(value, to.get(type))) return false;
        to.set(type, value);
        return true;
    }

    private static DataComponentMap carriedOf(ItemStack payload) {
        var types = CARRIED.get();
        if (types.isEmpty()) return DataComponentMap.EMPTY;
        var components = DataComponentMap.builder();
        for (var type : types) put(components, type.get(), payload);
        return components.build();
    }

    private static <T> void put(
            DataComponentMap.Builder components, DataComponentType<T> type, ItemStack payload) {
        components.set(type, payload.get(type));
    }
}
