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

package com.hbm.tags;

import com.hbm.util.RegistryUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class HbmBlockTags {
    public static final TagKey<Block> DEPTH_ROCK =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("depth_rock"));

    public static final TagKey<Block> ANCIENT_TOMB_MATERIAL =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("ancient_tomb_material"));

    public static final TagKey<Block> METEORITE_REPLACEABLE =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("meteorite_replaceable"));

    public static final TagKey<Block> FBI_BREAKABLE =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("fbi_breakable"));

    public static final TagKey<Block> METAL_FENCE_JOINS =
            TagKey.create(Registries.BLOCK, RegistryUtil.id("metal_fence_joins"));

    private HbmBlockTags() {}
}
