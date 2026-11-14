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

package com.hbm.world.gen.nbt;

import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public final class DungeonProcessorLists {

    public static final ResourceKey<StructureProcessorList> BARREL = key("barrel");
    public static final ResourceKey<StructureProcessorList> DESERT_ATOM = key("desert_atom");
    public static final ResourceKey<StructureProcessorList> LIBRARY_DUNGEON =
            key("library_dungeon");
    public static final ResourceKey<StructureProcessorList> SPACESHIP = key("spaceship");

    private DungeonProcessorLists() {}

    public static List<StructureProcessor> forTemplate(LevelReader level, Identifier templateId) {
        return level.registryAccess()
                .lookupOrThrow(Registries.PROCESSOR_LIST)
                .get(ResourceKey.create(Registries.PROCESSOR_LIST, templateId))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "template "
                                                + templateId
                                                + " declares no processor_list, so its markers would place unresolved"))
                .value()
                .list();
    }

    private static ResourceKey<StructureProcessorList> key(String path) {
        return ResourceKey.create(Registries.PROCESSOR_LIST, RegistryUtil.id(path));
    }
}
