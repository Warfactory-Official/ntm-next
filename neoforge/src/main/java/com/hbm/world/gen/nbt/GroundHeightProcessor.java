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

import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class GroundHeightProcessor extends GravityProcessor {
    public static final MapCodec<GravityProcessor> MAP_CODEC =
            Codec.INT
                    .optionalFieldOf("offset", 0)
                    .xmap(
                            GroundHeightProcessor::new,
                            processor -> ((GroundHeightProcessor) processor).offset);
    private final int offset;

    public GroundHeightProcessor(int offset) {
        super(Heightmap.Types.OCEAN_FLOOR_WG, offset);
        this.offset = offset;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        return processedBlockInfo;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos position,
            BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
            StructurePlaceSettings settings) {
        BoundingBox box = settings.getBoundingBox();
        Long2IntOpenHashMap grounds = new Long2IntOpenHashMap();
        grounds.defaultReturnValue(Integer.MIN_VALUE);
        for (int i = 0; i < processedBlockInfoList.size(); i++) {
            StructureTemplate.StructureBlockInfo info = processedBlockInfoList.get(i);
            BlockPos pos = info.pos();
            if (box != null
                    && (pos.getX() < box.minX()
                            || pos.getX() > box.maxX()
                            || pos.getZ() < box.minZ()
                            || pos.getZ() > box.maxZ())) continue;
            long column = ChunkPos.pack(pos.getX(), pos.getZ());
            int ground = grounds.get(column);
            if (ground == Integer.MIN_VALUE) {
                ground = WorldgenHeight.ground(level, pos.getX(), pos.getZ());
                grounds.put(column, ground);
            }
            int y = ground + offset + pos.getY() - position.getY();
            processedBlockInfoList.set(
                    i,
                    new StructureTemplate.StructureBlockInfo(
                            new BlockPos(pos.getX(), y, pos.getZ()), info.state(), info.nbt()));
        }
        return processedBlockInfoList;
    }

    @Override
    public MapCodec<GravityProcessor> codec() {
        return MAP_CODEC;
    }
}
