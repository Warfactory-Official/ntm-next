// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tags;

import com.hbm.lib.Library;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HbmItemTags {

    public static final TagKey<Item> FRAMED_MESH =
            TagKey.create(Registries.ITEM, Library.id("framed_mesh"));

    public static final TagKey<Item> FRAMED_UNBOBBED =
            TagKey.create(Registries.ITEM, Library.id("framed_unbobbed"));

    private HbmItemTags() {}
}
