// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.material;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mov.movblock.tenon.strip.api.DropSafe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public class MaterialShapes {

    public static final List<MaterialShapes> allShapes = new ArrayList<>();

    public static final MaterialShapes ANY = new MaterialShapes(0, "c", null);
    public static final MaterialShapes ONLY_ORE = new MaterialShapes(0, "c", null);

    public static final MaterialShapes ORE = new MaterialShapes(0, "c", "ores");

    public static final MaterialShapes QUANTUM = new MaterialShapes(1, "c", null);

    public static final MaterialShapes NUGGET = new MaterialShapes(8, "c", "nuggets");
    public static final MaterialShapes DUSTTINY =
            new MaterialShapes(NUGGET.quantity, "c", "tiny_dusts");
    public static final MaterialShapes BILLET =
            new MaterialShapes(NUGGET.quantity * 6, "hbm", "billets");
    public static final MaterialShapes GEM = new MaterialShapes(72, "c", "gems");
    public static final MaterialShapes CRYSTAL = new MaterialShapes(72, "c", "crystals");
    public static final MaterialShapes DUST = new MaterialShapes(72, "c", "dusts");
    public static final MaterialShapes PLATE = new MaterialShapes(72, "c", "plates");
    public static final MaterialShapes QUART = new MaterialShapes(162, "c", null);
    public static final MaterialShapes BLOCK = new MaterialShapes(648, "c", "storage_blocks");

    public static final MaterialShapes BOLT = new MaterialShapes(9, "c", "bolts").autogen("bolt");
    public static final MaterialShapes FRAGMENT =
            new MaterialShapes(8, "hbm", "bedrock_ore_fragments").autogen("bedrock_ore_fragment");
    public static final MaterialShapes SHELL =
            new MaterialShapes(72 * 4, "hbm", "shells").autogen("shell");
    public static final MaterialShapes PIPE =
            new MaterialShapes(72 * 3, "hbm", "pipes").autogen("pipe");
    public static final MaterialShapes INGOT =
            new MaterialShapes(72, "c", "ingots").autogen("ingot_raw");
    public static final MaterialShapes CASTPLATE =
            new MaterialShapes(72 * 3, "hbm", "plates_cast").autogen("plate_cast");
    public static final MaterialShapes WELDEDPLATE =
            new MaterialShapes(72 * 6, "hbm", "plates_welded").autogen("plate_welded");
    public static final MaterialShapes WIRE =
            new MaterialShapes(9, "c", "fine_wires").autogen("wire_fine");
    public static final MaterialShapes DENSEWIRE =
            new MaterialShapes(72, "hbm", "wires_dense").autogen("wire_dense");
    public static final MaterialShapes LIGHTBARREL =
            new MaterialShapes(72 * 3, "hbm", "barrels_light").autogen("part_barrel_light");
    public static final MaterialShapes HEAVYBARREL =
            new MaterialShapes(72 * 6, "hbm", "barrels_heavy").autogen("part_barrel_heavy");
    public static final MaterialShapes LIGHTRECEIVER =
            new MaterialShapes(72 * 4, "hbm", "receivers_light").autogen("part_receiver_light");
    public static final MaterialShapes HEAVYRECEIVER =
            new MaterialShapes(72 * 9, "hbm", "receivers_heavy").autogen("part_receiver_heavy");
    public static final MaterialShapes MECHANISM =
            new MaterialShapes(72 * 4, "hbm", "gun_mechanisms").autogen("part_mechanism");
    public static final MaterialShapes STOCK =
            new MaterialShapes(72 * 4, "hbm", "stocks").autogen("part_stock");
    public static final MaterialShapes GRIP =
            new MaterialShapes(72 * 2, "hbm", "grips").autogen("part_grip");

    public static final Set<MaterialShapes> HAZARDABLE_SHAPES =
            Set.of(
                    NUGGET, INGOT, DUSTTINY, DUST, GEM, CRYSTAL, PLATE, CASTPLATE, BILLET, BLOCK,
                    ORE);

    public final int quantity;
    public final String namespace;
    public final @Nullable String tagPathPlural;
    public @Nullable String autogenItemPrefix;

    private MaterialShapes(int quantity, String namespace, @Nullable String tagPathPlural) {
        this.quantity = quantity;
        this.namespace = namespace;
        this.tagPathPlural = tagPathPlural;
        allShapes.add(this);
    }

    public MaterialShapes autogen(String prefix) {
        this.autogenItemPrefix = prefix;
        return this;
    }

    public int q(int amount) {
        return this.quantity * amount;
    }

    public int q(int unitsUsed, int itemsProduced) {
        return this.quantity * unitsUsed / itemsProduced;
    }

    public String name() {
        return tagPathPlural != null ? tagPathPlural : "unknown";
    }

    @DropSafe
    public TagKey<Item> tagFor(String segment) {
        if (tagPathPlural == null)
            throw new IllegalStateException("Shape has no tag path: " + this);
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(namespace, tagPathPlural + "/" + segment));
    }

    public TagKey<Item> tagForPrivate(String segment) {
        if (tagPathPlural == null)
            throw new IllegalStateException("Shape has no tag path: " + this);
        return TagKey.create(Registries.ITEM, Library.id(tagPathPlural + "/" + segment));
    }

    public Identifier autogenItemId(String segment) {
        if (autogenItemPrefix == null)
            throw new IllegalStateException("Shape is not autogen-eligible: " + this);
        return Library.id(autogenItemPrefix + "_" + segment);
    }
}
