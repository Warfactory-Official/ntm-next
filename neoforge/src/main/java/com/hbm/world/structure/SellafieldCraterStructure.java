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

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class SellafieldCraterStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<SellafieldCraterStructure> CODEC =
            RecordCodecBuilder.<SellafieldCraterStructure>mapCodec(
                            i ->
                                    i.group(
                                                    settingsCodec(i),
                                                    Codec.intRange(1, 256)
                                                            .optionalFieldOf("min_size", 8)
                                                            .forGetter(s -> s.minSize),
                                                    Codec.intRange(2, 256)
                                                            .optionalFieldOf("max_size", 64)
                                                            .forGetter(s -> s.maxSize),
                                                    Codec.intRange(1, 8)
                                                            .optionalFieldOf("exponent", 2)
                                                            .forGetter(s -> s.exponent),
                                                    BlockState.CODEC
                                                            .optionalFieldOf("regolith")
                                                            .forGetter(s -> s.regolith),
                                                    BlockState.CODEC
                                                            .optionalFieldOf("rock")
                                                            .forGetter(s -> s.rock))
                                            .apply(i, SellafieldCraterStructure::new))
                    .validate(
                            structure ->
                                    structure.maxSize > structure.minSize
                                            ? DataResult.success(structure)
                                            : DataResult.error(
                                                    () -> "max_size must exceed min_size"));

    private final int minSize;
    private final int maxSize;
    private final int exponent;
    private final Optional<BlockState> regolith;
    private final Optional<BlockState> rock;

    public SellafieldCraterStructure(Structure.StructureSettings settings) {
        this(settings, 8, 64, 2, Optional.empty(), Optional.empty());
    }

    public SellafieldCraterStructure(
            Structure.StructureSettings settings,
            int minSize,
            int maxSize,
            int exponent,
            Optional<BlockState> regolith,
            Optional<BlockState> rock) {
        super(settings);
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.exponent = exponent;
        this.regolith = regolith;
        this.rock = rock;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getMiddleBlockX();
        int blockZ = chunkPos.getMiddleBlockZ();
        if (!ColumnBiome.valid(context, blockX, blockZ)) return Optional.empty();
        BlockPos origin = new BlockPos(blockX, 0, blockZ);

        int radius = minSize + context.random().nextInt(maxSize - minSize);
        int minY = context.heightAccessor().getMinY();
        int maxY = context.heightAccessor().getMaxY();

        return Optional.of(
                new Structure.GenerationStub(
                        origin,
                        builder ->
                                builder.addPiece(
                                        new SellafieldCraterPiece(
                                                origin, radius, minY, maxY, exponent, regolith,
                                                rock))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SELLAFIELD_CRATER.get();
    }

    public int maxRadius() {
        return maxSize - 1;
    }
}
