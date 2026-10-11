// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public final class MarkerTableProcessor implements StructureProcessor {
    public static final MapCodec<MarkerTableProcessor> MAP_CODEC =
            StructureProcessorType.SINGLE_CODEC
                    .listOf()
                    .comapFlatMap(
                            MarkerTableProcessor::decode,
                            table -> List.<StructureProcessor>copyOf(table.members))
                    .fieldOf("processors");

    private final List<MarkerProcessor> members;
    private final Map<BlockState, int[]> byMarker = new Reference2ObjectOpenHashMap<>();

    public MarkerTableProcessor(List<MarkerProcessor> members) {
        this.members = List.copyOf(members);
        for (int i = 0; i < this.members.size(); i++) {
            int[] at = this.byMarker.get(this.members.get(i).marker());
            at = at == null ? new int[] {i} : Arrays.copyOf(at, at.length + 1);
            at[at.length - 1] = i;
            this.byMarker.put(this.members.get(i).marker(), at);
        }
    }

    private static DataResult<MarkerTableProcessor> decode(List<StructureProcessor> processors) {
        List<MarkerProcessor> members = new ArrayList<>(processors.size());
        for (StructureProcessor processor : processors) {
            if (!(processor instanceof MarkerProcessor member)) {
                return DataResult.error(
                        () -> "hbm:marker_table holds only marker processors, not " + processor);
            }
            members.add(member);
        }
        return DataResult.success(new MarkerTableProcessor(members));
    }

    public static List<StructureProcessor> fold(List<StructureProcessor> chain) {
        List<StructureProcessor> out = new ArrayList<>(chain.size());
        List<MarkerProcessor> run = new ArrayList<>();
        for (StructureProcessor processor : chain) {
            if (processor instanceof MarkerProcessor member) {
                run.add(member);
                continue;
            }
            flush(run, out);
            out.add(processor);
        }
        flush(run, out);
        return List.copyOf(out);
    }

    private static void flush(List<MarkerProcessor> run, List<StructureProcessor> out) {
        if (run.size() == 1) out.add(run.getFirst());
        else if (run.size() > 1) out.add(new MarkerTableProcessor(run));
        run.clear();
    }

    public List<MarkerProcessor> members() {
        return this.members;
    }

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        StructureTemplate.StructureBlockInfo current = processedBlockInfo;
        int last = -1;
        while (current != null) {
            int next = next(current.state(), last);
            if (next < 0) return current;
            current =
                    this.members
                            .get(next)
                            .processBlock(
                                    level,
                                    targetPosition,
                                    referencePos,
                                    templateRelativePos,
                                    current,
                                    settings);
            last = next;
        }
        return null;
    }

    private int next(BlockState state, int after) {
        int[] at = this.byMarker.get(state);
        if (at == null) return -1;
        for (int index : at) {
            if (index > after) return index;
        }
        return -1;
    }

    @Override
    public MapCodec<MarkerTableProcessor> codec() {
        return MAP_CODEC;
    }
}
