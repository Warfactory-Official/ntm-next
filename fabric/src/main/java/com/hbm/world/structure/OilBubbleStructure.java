// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class OilBubbleStructure extends Structure {

    public static final MapCodec<OilBubbleStructure> CODEC =
            RecordCodecBuilder.<OilBubbleStructure>mapCodec(
                            i ->
                                    i.group(
                                                    settingsCodec(i),
                                                    BlockState.CODEC
                                                            .fieldOf("core")
                                                            .forGetter(s -> s.core),
                                                    RuleTest.CODEC
                                                            .fieldOf("replaceable")
                                                            .forGetter(s -> s.replaceable),
                                                    Codec.intRange(0, Integer.MAX_VALUE)
                                                            .fieldOf("min_size")
                                                            .forGetter(s -> s.minSize),
                                                    Codec.intRange(1, Integer.MAX_VALUE)
                                                            .fieldOf("max_size")
                                                            .forGetter(s -> s.maxSize),
                                                    Codec.BOOL
                                                            .fieldOf("fuzzy")
                                                            .forGetter(s -> s.fuzzy),
                                                    Codec.INT
                                                            .fieldOf("min_y")
                                                            .forGetter(s -> s.minY),
                                                    Codec.intRange(1, Integer.MAX_VALUE)
                                                            .fieldOf("range_y")
                                                            .forGetter(s -> s.rangeY))
                                            .apply(i, OilBubbleStructure::new))
                    .validate(
                            structure ->
                                    structure.maxSize > structure.minSize
                                            ? DataResult.success(structure)
                                            : DataResult.error(
                                                    () -> "max_size must exceed min_size"));

    private final BlockState core;
    private final RuleTest replaceable;
    private final int minSize;
    private final int maxSize;
    private final boolean fuzzy;
    private final int minY;
    private final int rangeY;

    public OilBubbleStructure(
            Structure.StructureSettings settings,
            BlockState core,
            RuleTest replaceable,
            int minSize,
            int maxSize,
            boolean fuzzy,
            int minY,
            int rangeY) {
        super(settings);
        this.core = core;
        this.replaceable = replaceable;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.fuzzy = fuzzy;
        this.minY = minY;
        this.rangeY = rangeY;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        var random = context.random();

        BlockPos centre =
                new BlockPos(
                        chunkPos.getMinBlockX() + random.nextInt(16),
                        minY + random.nextInt(rangeY),
                        chunkPos.getMinBlockZ() + random.nextInt(16));
        int radius = minSize + random.nextInt(maxSize - minSize);

        long scarSeed = random.nextLong();

        return Optional.of(
                new Structure.GenerationStub(
                        centre,
                        builder ->
                                builder.addPiece(
                                        new OilBubblePiece(
                                                centre,
                                                radius,
                                                fuzzy,
                                                core,
                                                replaceable,
                                                scarSeed))));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.OIL_BUBBLE.get();
    }
}
