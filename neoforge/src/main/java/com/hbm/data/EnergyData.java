// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class EnergyData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("energy_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group ENERGY = DataGroups.group(GROUPS, "energy");
    public static final DataGroups.Param<Integer> ENERGY_RATIO_HE = ENERGY.positive("he_ratio", 5);
    public static final DataGroups.Param<Integer> ENERGY_RATIO_FE = ENERGY.positive("fe_ratio", 1);

    private EnergyData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
