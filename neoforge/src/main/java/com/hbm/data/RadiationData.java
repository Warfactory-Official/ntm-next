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

package com.hbm.data;

import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class RadiationData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("radiation_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group RADIATION = DataGroups.group(GROUPS, "radiation");
    public static final DataGroups.Param<Double> FOG_RAD = RADIATION.real("fog_rad", 100D);
    public static final DataGroups.Param<Double> FOG_CHANCE =
            RADIATION.atLeast("fog_chance", 20D, 1D);
    public static final DataGroups.Param<Boolean> WORLD_RAD_EFFECTS =
            RADIATION.bool("world_rad_effects", true);
    public static final DataGroups.Param<Boolean> CLEANUP_DEAD_DIRT =
            RADIATION.bool("cleanup_dead_dirt", false);
    public static final DataGroups.Param<Boolean> ENABLE_CONTAMINATION =
            RADIATION.bool("enable_contamination", true);
    public static final DataGroups.Param<Boolean> ENABLE_CONTAMINATION_ON_GROUND =
            RADIATION.bool("enable_contamination_on_ground", true);
    public static final DataGroups.Param<Boolean> ENABLE_CHUNK_RADS =
            RADIATION.bool("enable_chunk_rads", true);
    public static final DataGroups.Param<Boolean> NEUTRON_ACTIVATION =
            RADIATION.bool("neutron_activation", false);
    public static final DataGroups.Param<Integer> NEUTRON_ACTIVATION_THRESHOLD =
            RADIATION.integer("neutron_activation_threshold", 15);
    public static final DataGroups.Param<Double> RAD_DIFFUSIVITY =
            RADIATION.atLeast("diffusivity", 10D, 0.000001D);
    public static final DataGroups.Param<Double> RAD_HALF_LIFE_SECONDS =
            RADIATION.atLeast("half_life_seconds", 120D, 0.000001D);

    private static final DataGroups.Group HAZARDS = DataGroups.group(GROUPS, "hazards");
    public static final DataGroups.Param<Boolean> DISABLE_ASBESTOS =
            HAZARDS.bool("disable_asbestos", false);
    public static final DataGroups.Param<Boolean> DISABLE_COAL =
            HAZARDS.bool("disable_coal", false);
    public static final DataGroups.Param<Boolean> DISABLE_HOT = HAZARDS.bool("disable_hot", false);
    public static final DataGroups.Param<Boolean> DISABLE_EXPLOSIVE =
            HAZARDS.bool("disable_explosive", false);
    public static final DataGroups.Param<Boolean> DISABLE_HYDRO =
            HAZARDS.bool("disable_hydro", false);
    public static final DataGroups.Param<Boolean> DISABLE_BLINDING =
            HAZARDS.bool("disable_blinding", false);
    public static final DataGroups.Param<Boolean> DISABLE_COLD =
            HAZARDS.bool("disable_cold", false);
    public static final DataGroups.Param<Boolean> DISABLE_TOXIC =
            HAZARDS.bool("disable_toxic", false);
    public static final DataGroups.Param<Boolean> ENABLE_POLLUTION =
            HAZARDS.bool("enable_pollution", true);
    public static final DataGroups.Param<Boolean> ENABLE_LEAD_FROM_BLOCKS =
            HAZARDS.bool("enable_lead_from_blocks", true);
    public static final DataGroups.Param<Boolean> ENABLE_LEAD_POISONING =
            HAZARDS.bool("enable_lead_poisoning", true);
    public static final DataGroups.Param<Boolean> ENABLE_SOOT_FOG =
            HAZARDS.bool("enable_soot_fog", true);
    public static final DataGroups.Param<Boolean> ENABLE_POISON =
            HAZARDS.bool("enable_poison", true);
    public static final DataGroups.Param<Double> POLLUTION_MULT =
            HAZARDS.real("pollution_mult", 1D);
    public static final DataGroups.Param<Double> BUFF_MOB_THRESHOLD =
            HAZARDS.bounded("buff_mob_threshold", 15D, -Double.MAX_VALUE, Double.MAX_VALUE);
    public static final DataGroups.Param<Double> SOOT_FOG_THRESHOLD =
            HAZARDS.bounded("soot_fog_threshold", 35D, -Double.MAX_VALUE, Double.MAX_VALUE);
    public static final DataGroups.Param<Double> SOOT_FOG_DIVISOR =
            HAZARDS.atLeast("soot_fog_divisor", 120D, Double.MIN_VALUE);

    private RadiationData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
