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

package com.hbm.world.structure;

import com.hbm.world.gen.nbt.DungeonProcessorLists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class TemplateStructurePiece extends StructurePiece {

    private final Identifier templateId;
    private final StructureTemplate template;

    public TemplateStructurePiece(
            StructurePieceType type,
            Identifier templateId,
            StructureTemplate template,
            BlockPos origin) {
        super(type, 0, boundingBoxFor(origin, template.getSize()));
        this.templateId = templateId;
        this.template = template;
    }

    public TemplateStructurePiece(
            StructurePieceType type, StructurePieceSerializationContext context, CompoundTag tag) {
        super(type, tag);
        this.templateId =
                Identifier.parse(
                        tag.getString("TemplateId")
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "TemplateStructurePiece NBT missing TemplateId")));
        this.template =
                context.structureTemplateManager()
                        .get(templateId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Structure piece needs template "
                                                        + templateId
                                                        + ", which is not registered"));
    }

    private static BoundingBox boundingBoxFor(BlockPos origin, Vec3i size) {
        return new BoundingBox(
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + size.getX() - 1,
                origin.getY() + size.getY() - 1,
                origin.getZ() + size.getZ() - 1);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("TemplateId", templateId.toString());
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {
        BlockPos origin =
                new BlockPos(
                        this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ());
        StructurePlaceSettings settings = new StructurePlaceSettings().setBoundingBox(chunkBB);
        for (StructureProcessor processor : DungeonProcessorLists.forTemplate(level, templateId)) {
            settings.addProcessor(processor);
        }
        this.template.placeInWorld(level, origin, origin, settings, random, 2);
    }
}
