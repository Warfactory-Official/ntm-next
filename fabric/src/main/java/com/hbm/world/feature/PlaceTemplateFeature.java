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

package com.hbm.world.feature;

import com.hbm.util.RegistryUtil;
import com.hbm.world.gen.nbt.DungeonProcessorLists;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public abstract class PlaceTemplateFeature extends Feature<NoneFeatureConfiguration> {
    private final Identifier templateId;

    protected PlaceTemplateFeature(String templateId) {
        super(NoneFeatureConfiguration.CODEC);
        this.templateId = RegistryUtil.id(templateId);
    }

    protected static boolean isSolid(BlockState state) {
        return state.isSolid();
    }

    protected abstract List<BlockPos> guardOffsets();

    protected abstract boolean isValidSpawn(WorldGenLevel level, BlockPos pos);

    protected BlockPos templateOrigin(BlockPos featureOrigin) {
        return featureOrigin;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        BlockPos.MutableBlockPos guard = new BlockPos.MutableBlockPos();

        for (BlockPos offset : guardOffsets()) {
            guard.setWithOffset(origin, offset);
            if (!isValidSpawn(level, guard)) return false;
        }

        StructureTemplateManager manager = level.getLevel().getStructureManager();
        Optional<StructureTemplate> template = manager.get(templateId);
        if (template.isEmpty()) return false;

        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (StructureProcessor processor : DungeonProcessorLists.forTemplate(level, templateId)) {
            settings.addProcessor(processor);
        }
        BlockPos placeOrigin = templateOrigin(origin);
        return template.get()
                .placeInWorld(level, placeOrigin, placeOrigin, settings, ctx.random(), 2);
    }
}
