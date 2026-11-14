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

import com.hbm.util.RegistryUtil;
import com.hbm.world.LocationIsValidSpawn;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class DesertAtomStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<DesertAtomStructure> CODEC = simpleCodec(DesertAtomStructure::new);

    private static final Identifier TEMPLATE_ID = RegistryUtil.id("desert_atom");
    private static final int ANCHOR_DY = -5;

    public DesertAtomStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        Optional<StructureTemplate> template = context.structureTemplateManager().get(TEMPLATE_ID);
        if (template.isEmpty()) return Optional.empty();

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        int y =
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        if (!ColumnBiome.valid(context, x, y, z)) return Optional.empty();

        NoiseColumn column =
                context.chunkGenerator()
                        .getBaseColumn(
                                x + 20, z + 16, context.heightAccessor(), context.randomState());
        if (!column.getBlock(y).isAir()
                || !LocationIsValidSpawn.isDryGround(column.getBlock(y - 1))) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, y + ANCHOR_DY, z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin,
                        builder ->
                                builder.addPiece(
                                        new TemplateStructurePiece(
                                                HbmStructureTypes.DESERT_ATOM_PIECE.get(),
                                                TEMPLATE_ID,
                                                template.get(),
                                                origin))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.DESERT_ATOM.get();
    }
}
