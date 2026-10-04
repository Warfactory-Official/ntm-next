// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tags;

import com.hbm.lib.Library;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HbmItemTags {

    public static final TagKey<Item> COAL_FUELS =
            TagKey.create(Registries.ITEM, Library.id("coal_fuels"));

    public static final TagKey<Item> NO_CONTAINER_NESTING =
            TagKey.create(Registries.ITEM, Library.id("no_container_nesting"));

    public static final TagKey<Item> FRAMED_MESH =
            TagKey.create(Registries.ITEM, Library.id("framed_mesh"));

    public static final TagKey<Item> FRAMED_UNBOBBED =
            TagKey.create(Registries.ITEM, Library.id("framed_unbobbed"));

    public static final TagKey<Item> FARADAY_ARMOR =
            TagKey.create(Registries.ITEM, Library.id("faraday_armor"));

    private HbmItemTags() {}
}
