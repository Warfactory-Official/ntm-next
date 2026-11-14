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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypeIds;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record SpawnerEntityProcessor(WeightedList<EntityType<?>> mobs)
        implements StructureProcessor {

    public static final MapCodec<SpawnerEntityProcessor> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            WeightedList.nonEmptyCodec(
                                                            BuiltInRegistries.ENTITY_TYPE
                                                                    .byNameCodec())
                                                    .fieldOf("mobs")
                                                    .forGetter(SpawnerEntityProcessor::mobs))
                                    .apply(i, SpawnerEntityProcessor::new));

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (!processedBlockInfo.state().is(Blocks.SPAWNER)) return processedBlockInfo;

        BlockPos pos = processedBlockInfo.pos();
        EntityType<?> mob =
                ProcessorRandom.weighted(
                        level,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        ProcessorRandom.SPAWNER_ENTITY,
                        mobs);
        CompoundTag entity = new CompoundTag();
        entity.putString("id", EntityType.getKey(mob).toString());
        CompoundTag spawnData = new CompoundTag();
        spawnData.put(SpawnData.ENTITY_TAG, entity);
        CompoundTag nbt =
                processedBlockInfo.nbt() == null
                        ? new CompoundTag()
                        : processedBlockInfo.nbt().copy();
        nbt.putString("id", BlockEntityTypeIds.MOB_SPAWNER.identifier().toString());
        nbt.put(BaseSpawner.SPAWN_DATA_TAG, spawnData);
        return new StructureTemplate.StructureBlockInfo(pos, processedBlockInfo.state(), nbt);
    }

    @Override
    public MapCodec<SpawnerEntityProcessor> codec() {
        return MAP_CODEC;
    }
}
