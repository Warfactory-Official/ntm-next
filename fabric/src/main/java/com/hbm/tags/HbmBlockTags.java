// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tags;

import com.hbm.lib.Library;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class HbmBlockTags {

    public static final TagKey<Block> DEPTH_ROCK =
            TagKey.create(Registries.BLOCK, Library.id("depth_rock"));

    public static final TagKey<Block> ANCIENT_TOMB_MATERIAL =
            TagKey.create(Registries.BLOCK, Library.id("ancient_tomb_material"));

    public static final TagKey<Block> METEORITE_REPLACEABLE =
            TagKey.create(Registries.BLOCK, Library.id("meteorite_replaceable"));

    public static final TagKey<Block> FBI_BREAKABLE =
            TagKey.create(Registries.BLOCK, Library.id("fbi_breakable"));

    private HbmBlockTags() {}
}
