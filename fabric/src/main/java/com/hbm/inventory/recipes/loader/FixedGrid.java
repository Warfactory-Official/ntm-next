// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record FixedGrid(CountIngredient[] inputs, int[] cells) {

    public static final MapCodec<FixedGrid> MAP_CODEC =
            RecordCodecBuilder.<Pattern>mapCodec(
                            i ->
                                    i.group(
                                                    Codec.STRING
                                                            .listOf()
                                                            .fieldOf("pattern")
                                                            .forGetter(Pattern::rows),
                                                    Codec.unboundedMap(
                                                                    Codec.STRING,
                                                                    CountIngredient.CODEC)
                                                            .fieldOf("key")
                                                            .forGetter(Pattern::key))
                                            .apply(i, Pattern::new))
                    .flatXmap(FixedGrid::decode, FixedGrid::encode);

    private static DataResult<FixedGrid> decode(Pattern pattern) {
        if (pattern.rows().size() != 3
                || pattern.rows().stream().anyMatch(row -> row.length() != 3)) {
            return DataResult.error(() -> "pattern must contain exactly three rows of three cells");
        }
        for (String symbol : pattern.key().keySet()) {
            if (symbol.length() != 1 || symbol.equals(" ")) {
                return DataResult.error(
                        () -> "key symbols must be one non-space character: '" + symbol + "'");
            }
        }
        Set<String> unused = new HashSet<>(pattern.key().keySet());
        List<CountIngredient> inputs = new ArrayList<>();
        List<Integer> cells = new ArrayList<>();
        for (int cell = 0; cell < 9; cell++) {
            char character = pattern.rows().get(cell / 3).charAt(cell % 3);
            if (character == ' ') continue;
            String symbol = String.valueOf(character);
            CountIngredient input = pattern.key().get(symbol);
            if (input == null)
                return DataResult.error(() -> "pattern names undefined key '" + symbol + "'");
            unused.remove(symbol);
            inputs.add(input);
            cells.add(cell);
        }
        if (!unused.isEmpty()) return DataResult.error(() -> "Unused pattern keys: " + unused);
        return DataResult.success(
                new FixedGrid(
                        inputs.toArray(CountIngredient[]::new),
                        cells.stream().mapToInt(Integer::intValue).toArray()));
    }

    private static DataResult<Pattern> encode(FixedGrid grid) {
        if (grid.inputs().length != grid.cells().length) {
            return DataResult.error(() -> "grid cells must pair one-for-one with input items");
        }
        char[] cells = "         ".toCharArray();
        Map<String, CountIngredient> key = new LinkedHashMap<>();
        for (int i = 0; i < grid.cells().length; i++) {
            int cell = grid.cells()[i];
            if (cell < 0 || cell >= 9 || cells[cell] != ' ') {
                return DataResult.error(() -> "grid cell must be unique and in 0..8: " + cell);
            }
            char symbol = (char) ('A' + i);
            cells[cell] = symbol;
            key.put(String.valueOf(symbol), grid.inputs()[i]);
        }
        return DataResult.success(
                new Pattern(
                        List.of(
                                new String(cells, 0, 3),
                                new String(cells, 3, 3),
                                new String(cells, 6, 3)),
                        key));
    }

    private record Pattern(List<String> rows, Map<String, CountIngredient> key) {}
}
