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
import net.minecraft.world.item.Item;

public final class HbmItemTags {

    public static final TagKey<Item> COAL_FUELS =
            TagKey.create(Registries.ITEM, RegistryUtil.id("coal_fuels"));

    public static final TagKey<Item> NO_CONTAINER_NESTING =
            TagKey.create(Registries.ITEM, RegistryUtil.id("no_container_nesting"));

    public static final TagKey<Item> FRAMED_MESH =
            TagKey.create(Registries.ITEM, RegistryUtil.id("framed_mesh"));

    public static final TagKey<Item> FRAMED_UNBOBBED =
            TagKey.create(Registries.ITEM, RegistryUtil.id("framed_unbobbed"));

    public static final TagKey<Item> FARADAY_ARMOR =
            TagKey.create(Registries.ITEM, RegistryUtil.id("faraday_armor"));

    private HbmItemTags() {}
}
