// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.lib.Library;
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
        return ResourceKey.create(Registries.PROCESSOR_LIST, Library.id(path));
    }
}
