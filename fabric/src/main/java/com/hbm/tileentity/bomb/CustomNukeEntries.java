// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.lib.Library;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class CustomNukeEntries {

    private static @Nullable Map<Item, Entry> entries;

    private CustomNukeEntries() {}

    public static @Nullable Entry of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return table().get(stack.getItem());
    }

    private static Map<Item, Entry> table() {
        Map<Item, Entry> built = entries;
        if (built != null) return built;
        built = new IdentityHashMap<>();

        add(built, "minecraft:gunpowder", Stage.TNT, 0.8F);
        add(built, "minecraft:tnt", Stage.TNT, 4F);
        add(built, "det_cord", Stage.TNT, 1.5F);
        add(built, "ingot_semtex", Stage.TNT, 8F);
        add(built, "det_charge", Stage.TNT, 15F);
        add(built, "red_barrel", Stage.TNT, 2.5F);
        add(built, "pink_barrel", Stage.TNT, 4F);
        add(built, "custom_tnt", Stage.TNT, 10F);

        add(built, "ingot_u233", Stage.NUKE, 15F);
        add(built, "ingot_u235", Stage.NUKE, 15F);
        add(built, "ingot_pu239", Stage.NUKE, 25F);
        add(built, "ingot_pu241", Stage.NUKE, 25F);
        add(built, "ingot_neptunium", Stage.NUKE, 30F);
        add(built, "nugget_u233", Stage.NUKE, 1.5F);
        add(built, "nugget_u235", Stage.NUKE, 1.5F);
        add(built, "nugget_pu239", Stage.NUKE, 2.5F);
        add(built, "nugget_pu241", Stage.NUKE, 2.5F);
        add(built, "nugget_neptunium", Stage.NUKE, 3.0F);
        add(built, "powder_neptunium", Stage.NUKE, 30F);
        add(built, "custom_nuke", Stage.NUKE, 30F);

        add(built, "cell_deuterium", Stage.HYDRO, 20F);
        add(built, "cell_tritium", Stage.HYDRO, 30F);
        add(built, "lithium", Stage.HYDRO, 20F);
        add(built, "custom_hydro", Stage.HYDRO, 30F);

        add(built, "cell_antimatter", Stage.AMAT, 5F);
        add(built, "custom_amat", Stage.AMAT, 15F);
        add(built, "egg_balefire_shard", Stage.AMAT, 15F);
        add(built, "egg_balefire", Stage.AMAT, 150F);

        add(built, "ingot_tungsten", Stage.DIRTY, 1F);
        add(built, "custom_dirty", Stage.DIRTY, 10F);

        add(built, "ingot_schrabidium", Stage.SCHRAB, 5F);
        add(built, "block_schrabidium", Stage.SCHRAB, 50F);
        add(built, "nugget_schrabidium", Stage.SCHRAB, 0.5F);
        add(built, "powder_schrabidium", Stage.SCHRAB, 5F);
        add(built, "cell_sas3", Stage.SCHRAB, 7.5F);
        add(built, "cell_anti_schrabidium", Stage.SCHRAB, 15F);
        add(built, "custom_schrab", Stage.SCHRAB, 15F);

        add(built, "nugget_euphemium", Stage.EUPH, 1F);
        add(built, "ingot_euphemium", Stage.EUPH, 1F);

        mult(built, "minecraft:redstone", Stage.TNT, 1.05F);
        mult(built, "minecraft:redstone_block", Stage.TNT, 1.5F);

        mult(built, "ingot_uranium", Stage.NUKE, 1.05F);
        mult(built, "ingot_plutonium", Stage.NUKE, 1.15F);
        mult(built, "ingot_u238", Stage.NUKE, 1.1F);
        mult(built, "ingot_pu238", Stage.NUKE, 1.15F);
        mult(built, "nugget_uranium", Stage.NUKE, 1.005F);
        mult(built, "nugget_plutonium", Stage.NUKE, 1.15F);
        mult(built, "nugget_u238", Stage.NUKE, 1.01F);
        mult(built, "nugget_pu238", Stage.NUKE, 1.015F);
        mult(built, "powder_uranium", Stage.NUKE, 1.05F);
        mult(built, "powder_plutonium", Stage.NUKE, 1.15F);

        mult(built, "ingot_pu240", Stage.DIRTY, 1.05F);
        mult(built, "nuclear_waste", Stage.DIRTY, 1.025F);
        mult(built, "block_waste", Stage.DIRTY, 1.25F);
        mult(built, "yellow_barrel", Stage.DIRTY, 1.2F);

        entries = built;
        return built;
    }

    private static void add(Map<Item, Entry> out, String id, Stage stage, float value) {
        out.put(item(id), new Entry(stage, value, false));
    }

    private static void mult(Map<Item, Entry> out, String id, Stage stage, float value) {
        out.put(item(id), new Entry(stage, value, true));
    }

    private static Item item(String id) {
        Identifier key = id.indexOf(':') < 0 ? Library.id(id) : Identifier.parse(id);
        return BuiltInRegistries.ITEM
                .getOptional(key)
                .orElseThrow(() -> new IllegalStateException(key + " is not registered"));
    }

    public enum Stage {
        TNT,
        NUKE,
        HYDRO,
        AMAT,
        DIRTY,
        SCHRAB,
        EUPH
    }

    public record Entry(Stage stage, float value, boolean multiplier) {}
}
