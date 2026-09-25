// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class ItemData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("item_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group ITEMS = DataGroups.group(GROUPS, "items");
    public static final DataGroups.Param<Boolean> CRATE_KEEP_CONTENTS =
            ITEMS.bool("crate_keeps_contents", true);
    public static final DataGroups.Param<Boolean> ENABLE_MKU = ITEMS.bool("enable_mku", true);
    public static final DataGroups.Param<Boolean> TAINT_TRAILS = ITEMS.bool("taint_trails", false);

    private static final DataGroups.Group DROPS = DataGroups.group(GROUPS, "drops");
    public static final DataGroups.Param<Boolean> DROP_ANTIMATTER_CELL =
            DROPS.bool("antimatter_cell_explodes", true);
    public static final DataGroups.Param<Boolean> DROP_SINGULARITY =
            DROPS.bool("singularity_spawns", true);
    public static final DataGroups.Param<Boolean> DROP_STAR =
            DROPS.bool("star_blaster_cell_explodes", true);
    public static final DataGroups.Param<Boolean> DROP_XEN_CRYSTAL =
            DROPS.bool("xen_crystal_moves_blocks", true);
    public static final DataGroups.Param<Boolean> DROP_DEAD_MAN =
            DROPS.bool("dead_man_explosive_detonates", true);

    private static final DataGroups.Group TOOLS = DataGroups.group(GROUPS, "tools");
    public static final DataGroups.Param<Boolean> TOOL_RECURSION_STONE =
            TOOLS.bool("veinminer_breaks_stone", false);
    public static final DataGroups.Param<Boolean> TOOL_RECURSION_NETHERRACK =
            TOOLS.bool("veinminer_breaks_netherrack", false);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_HAMMER =
            TOOLS.bool("ability_hammer", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_VEIN =
            TOOLS.bool("ability_vein", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_LUCK =
            TOOLS.bool("ability_luck", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_SILK =
            TOOLS.bool("ability_silk", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_FURNACE =
            TOOLS.bool("ability_furnace", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_SHREDDER =
            TOOLS.bool("ability_shredder", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_CENTRIFUGE =
            TOOLS.bool("ability_centrifuge", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_CRYSTALLIZER =
            TOOLS.bool("ability_crystallizer", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_MERCURY =
            TOOLS.bool("ability_mercury", true);
    public static final DataGroups.Param<Boolean> TOOL_ABILITY_EXPLOSION =
            TOOLS.bool("ability_explosion", true);

    private static final DataGroups.Group WEAPONS = DataGroups.group(GROUPS, "weapons");
    public static final DataGroups.Param<Boolean> ENABLE_GUNS = WEAPONS.bool("enable_guns", true);

    private static final DataGroups.Group PLAYER = DataGroups.group(GROUPS, "player");
    public static final DataGroups.Param<String> POTION_SICKNESS =
            PLAYER.text("potion_sickness", "OFF", "OFF", "NORMAL", "TERRARIA");

    private ItemData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
